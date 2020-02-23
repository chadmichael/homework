package com.blackdog

import java.io._

object CopyBytes extends App {
  var in = None: Option[FileInputStream]  // initialized to none
  var out = None: Option[FileOutputStream]
  
  try {
    in = Some(new FileInputStream("build.sbt"))
    out = Some(new FileOutputStream("build.sbt.copy"))
    var c = 0
    while ({c = in.get.read; c != -1}) {
      out.get.write(c)
    }
  } catch {
      case e: IOException => e.printStackTrace
  } finally {
    println("entered finally ... ")
    if (in.isDefined) in.get.close
    if (out.isDefined) out.get.close
  }
}
