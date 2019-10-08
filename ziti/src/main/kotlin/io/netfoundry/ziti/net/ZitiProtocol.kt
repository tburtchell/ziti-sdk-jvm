/*
 * Copyright (c) 2019 NetFoundry, Inc.
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

package io.netfoundry.ziti.net

/**
 *
 * @author eugene
 * @since 9/12/19
 */
object ZitiProtocol {
    val VERSION = byteArrayOf(0x3, 0x6, 0x9, 0xc)

    const val HEADER_LENGTH = 20

    object ContentType {
        const val HelloType: Int = 0
        const val PingType: Int = 1
        const val ResultType: Int = 2
        const val LatencyType: Int = 3

        //  EDGE
        const val Connect = 60783
        const val StateConnected = 60784
        const val StateClosed = 60785
        const val Data = 60786
        const val Dial = 60787
        const val DialSuccess = 60788
        const val DialFailed = 60789
        const val Bind = 60790
        const val Unbind = 60791
    }

    object Header {
        const val ConnectionId: Int = 0
        const val ReplyFor: Int = 1
        const val ResultSuccess: Int = 2
        const val HelloListener: Int = 3

        // Headers in the range 128-255 inclusive will be reflected when creating replies
        val ReflectedHeaderBitMask = (1 shl 7)
        val MaxReflectedHeader = (1 shl 8) - 1

        const val ConnId = 1000
        const val SeqHeader = 1001
        const val SessionToken = 1002
    }


}