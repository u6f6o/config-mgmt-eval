import groovy.json.JsonSlurper

/**
 * Created by tomcat on 5/29/15.
 */


def slurper = new JsonSlurper().parse(new URL("http://localhost:8500/v1/kv/?recurse"))

println slurper.size
