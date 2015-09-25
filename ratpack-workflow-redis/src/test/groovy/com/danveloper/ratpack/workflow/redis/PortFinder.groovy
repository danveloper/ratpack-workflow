package com.danveloper.ratpack.workflow.redis

class PortFinder {

  static int nextFree() {
    def ss = new ServerSocket()
    ss.bind(new InetSocketAddress(0))
    def port = ss.localPort
    ss.close()
    port
  }
}
