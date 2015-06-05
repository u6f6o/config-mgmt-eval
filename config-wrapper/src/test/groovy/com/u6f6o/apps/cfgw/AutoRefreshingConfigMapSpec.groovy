package com.u6f6o.apps.cfgw
import com.u6f6o.apps.cfgw.fetch.ConfigFetcher
import com.u6f6o.apps.cfgw.common.TimeSpan
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.CountDownLatch

class AutoRefreshingConfigMapSpec extends Specification {

    @Unroll("init fails-fast on #message")
    def "init fails-fast"(){
        setup:
            def clazzInTest = new AutoRefreshingConfigMap(fetcherMock,
                    TimeSpan.seconds(1),
                    TimeSpan.minutes(5))
        when:
            clazzInTest.initializeConfig()
        then:
            thrown(AssertionError)
        where:
            message             | fetcherMock
            "exceeded timeout"  | {-> sleep(5000); return [:]} as ConfigFetcher
            "thrown exception"  | {-> throw new IllegalStateException("whatsoever")} as ConfigFetcher
    }

    def "continue on refresh timeout"(){
        setup:
            def latch = new CountDownLatch(2)
            def fetcherMock = { ->
                latch.countDown() // this one should happen
                sleep(5000)
                latch.countDown() // this one not
                return [:]
            }
            def clazzInTest = new AutoRefreshingConfigMap(fetcherMock,
                    TimeSpan.minutes(60),
                    TimeSpan.millis(200))
        when:
            clazzInTest.scheduleConfigRefresh()
            sleep(350)
        then:
            1 == latch.getCount()
    }

    def "continue on refresh error"(){
        setup:
            def latch = new CountDownLatch(1)
            def fetcherMock = { ->
                latch.countDown() // this one should happen
                throw new IllegalStateException("whatsoever")
            }
            def clazzInTest = new AutoRefreshingConfigMap(fetcherMock,
                    TimeSpan.minutes(60),
                    TimeSpan.millis(200))
        when:
            clazzInTest.scheduleConfigRefresh()
            sleep(350)
        then:
            0 == latch.getCount()
    }

    def "init config and refresh later on"() {
        setup:
            ConfigFetcher fetcherMock = Mock()
            def clazzInTest = new AutoRefreshingConfigMap(fetcherMock,
                    TimeSpan.millis(100),
                    TimeSpan.millis(50))
        expect:
            null == clazzInTest.@configCache
        when:
            clazzInTest.initializeConfig()
            sleep(150)
        then:
            1 * fetcherMock.fetchLatestConfig() >> [name: 'Wallace', likes: 'cheese']
            "Wallace" == clazzInTest["name"]
            "cheese" == clazzInTest["likes"]
        when:
            clazzInTest.scheduleConfigRefresh()
            sleep(70)
        then:
            1 * fetcherMock.fetchLatestConfig() >> [name: 'Grommit', likes: 'dogfood']
            "Grommit" == clazzInTest["name"]
            "dogfood" == clazzInTest["likes"]
    }
}
