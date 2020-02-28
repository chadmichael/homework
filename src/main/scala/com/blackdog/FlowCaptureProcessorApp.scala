package com.blackdog

object FlowCaptureProcessorApp extends App {
  if (args.isEmpty) {
    println("Usage: netflowprocessor capture_files ...")
    println("  One or more paths to caputure files that should be processed.")
  } else {
    for (arg <- args)
      new FlowCaptureProcessor(arg).processRawNetFlowCapture
  }
}