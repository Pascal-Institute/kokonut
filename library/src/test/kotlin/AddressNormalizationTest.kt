import kokonut.util.Utility
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressNormalizationTest {

        @Test
        fun `replaces 0_0_0_0 with caller ip`() {
                val normalized =
                        Utility.normalizeNodeAddress(
                                address = "http://0.0.0.0:80",
                                remoteHost = "203.0.113.10"
                        )
                assertEquals("http://203.0.113.10:80", normalized)
        }

        @Test
        fun `uses x_forwarded_for first ip`() {
                val normalized =
                        Utility.normalizeNodeAddress(
                                address = "http://localhost:80",
                                remoteHost = "10.0.0.5",
                                forwardedForHeader = "198.51.100.2, 10.0.0.5"
                        )
                assertEquals("http://198.51.100.2:80", normalized)
        }

        @Test
        fun `keeps non wildcard host unchanged`() {
                val normalized =
                        Utility.normalizeNodeAddress(
                                address = "http://example.com:80",
                                remoteHost = "203.0.113.10"
                        )
                assertEquals("http://example.com:80", normalized)
        }

        @Test
        fun `accepts host_port without scheme`() {
                val normalized =
                        Utility.normalizeNodeAddress(
                                address = "0.0.0.0:80",
                                remoteHost = "203.0.113.10"
                        )
                assertEquals("http://203.0.113.10:80", normalized)
        }

        @Test
        fun `advertise url is preferred by default`() {
                val resolved =
                        Utility.resolveAdvertiseAddress(
                                bindHost = "0.0.0.0",
                                bindPort = 80,
                                advertiseUrlEnv = "http://kntfull.duckdns.org:80",
                                advertiseHostEnv = "ignored.example"
                        )
                assertEquals("http://kntfull.duckdns.org:80", resolved)
        }

        @Test
        fun `advertise url without scheme is normalized`() {
                val resolved =
                        Utility.resolveAdvertiseAddress(
                                bindHost = "0.0.0.0",
                                bindPort = 80,
                                advertiseUrlEnv = "kntfull.duckdns.org:80",
                                advertiseHostEnv = null
                        )
                assertEquals("http://kntfull.duckdns.org:80", resolved)
        }
}
