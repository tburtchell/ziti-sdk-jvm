/*
 * Copyright (c) 2018-2021 NetFoundry, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openziti.net.dns

import org.openziti.util.Logged
import org.openziti.util.ZitiLog
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Uses reflection to inject Ziti DNS into the InetAddress resolution path.
 *
 * This is probably fragile, as it relies on using reflection to expose internal APIs.
 */
internal abstract class NameService : InvocationHandler, Logged by ZitiLog() {

    abstract fun lookupAllHostAddr(host: String): Array<InetAddress>?
    abstract fun getHostByAddr(addr: ByteArray): String?

    override fun invoke(proxy: Any?, method: Method?, args: Array<Any?>): Any? {
        var result: Any? = null
        when (method?.name) {
            "lookupAllHostAddr" -> {
                val hostname:String = args[0] as String

                try {
                    result = lookupAllHostAddr(hostname)
                } catch (e:UnknownHostException) { /* Do Nothing*/ }

                if( null == result ) {
                    d("Falling back to JRE to resolve host $hostname")
                    result = lookupAllHostAddr.invoke(nameService,hostname)
                }
            }
            "getHostByAddr" -> {
                var address:ByteArray = args[0] as ByteArray

                try {
                    result = getHostByAddr(address)
                } catch (e:UnknownHostException) { /* Do Nothing*/ }

                if( null == result ) {
                    d("Falling back to JRE to resolve address $address")
                    result = getHostByAddr.invoke(nameService,address)
                }
            }
            else -> {
                var o: StringBuilder = StringBuilder()
                o.append(method?.returnType?.canonicalName).append(" ").append(method?.name).append("(")
                method?.parameterTypes?.forEachIndexed { i, p ->
                    if (i >= 0) o.append(", ")
                    o.append(p.canonicalName).append(" p").append(i)
                }
                o.append(")")
                throw UnsupportedOperationException(o.toString())
            }
        }
        return result;
    }

    companion object : Logged by ZitiLog() {
        private lateinit var lookupAllHostAddr: Method
        private lateinit var getHostByAddr: Method
        private lateinit var nameService: Any

        fun install(dns: NameService): Unit {
            d("Installing name service " + dns::class.java.canonicalName)
            val iNetAddressClass = InetAddress::class

            var zitiNameService: Any
            var nameServiceField: Field

            try {
                // JRE 9 and later
                var iface = Class.forName("java.net.InetAddress\$NameService")
                nameServiceField = InetAddress::class.java.getDeclaredField("nameService")
                nameServiceField.isAccessible = true
                nameService = nameServiceField.get(null)

                lookupAllHostAddr = iface.getDeclaredMethod("lookupAllHostAddr", String::class.java)
                lookupAllHostAddr.isAccessible = true

                getHostByAddr = iface.getDeclaredMethod("getHostByAddr", ByteArray::class.java)
                getHostByAddr.isAccessible = true

                zitiNameService = Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), dns);
                d("Ziti name service installed with JRE 9 compliance")
            } catch (e: Exception) {
                when (e) {
                    is ClassNotFoundException,
                    is NoSuchFieldException -> {
                        // JRE 8
                        var iface = Class.forName("sun.net.spi.nameservice.NameService")
                        nameServiceField = InetAddress::class.java.getDeclaredField("nameServices")
                        nameServiceField.isAccessible = true

                        nameService = (nameServiceField.get(null) as List<NameService>)[0]

                        lookupAllHostAddr = iface.getDeclaredMethod("lookupAllHostAddr", String::class.java)
                        lookupAllHostAddr.isAccessible = true

                        getHostByAddr = iface.getDeclaredMethod("getHostByAddr", ByteArray::class.java)
                        getHostByAddr.isAccessible = true

                        zitiNameService = listOf(Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), dns))
                        d("Ziti name service installed with JRE 8 compliance")
                    }
                    else -> throw e
                }
            }
            nameServiceField.set(InetAddress::class, zitiNameService);
        }
    }
}