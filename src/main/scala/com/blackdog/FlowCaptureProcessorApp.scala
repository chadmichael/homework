package com.blackdog

object FlowCaptureProcessorApp extends App {
  if (args.isEmpty) {
    println("Requires argument that is a path to a valid flow capture file")
  } else {
    new FlowCaptureProcessor(args(0)).processRawNetFlowCapture
  }
}