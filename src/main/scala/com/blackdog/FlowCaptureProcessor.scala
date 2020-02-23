package com.blackdog

import java.io.{FileInputStream, BufferedInputStream}
import java.nio.ByteBuffer
import resource._


// TODO Parallelism?  I think it would require a fairly sophisticated addition to this code.  The main sticking point being that the length 
// of the packets is only discovered as we process things.  In order to parallelize this, e.g. process packets in parallel, there would need 
// to be a pre-processor that chunked out the raw packets by processing the header at least.  And then I'm not even sure this makes sense.  
// It might be better to have multiple flow captures files and the parallelism would be on the raw file as the unit of work, rather than the 
// packets themselves.  

object FlowCaptureProcessor extends App {
  def processRawNetFlowCapture = {
    val filename = "flow_capture"
    
    // TODO figure out optimal buffer size, based upon observation of actual performance on specific hardware and systems.  
    val bufferSize = 64 * 1024
      
    // Using Scala ARM for higher level, safer management of resources
    for(inputStream <- managed(new BufferedInputStream(new FileInputStream(filename), bufferSize))) { 
      val packets = processFlowCapture(inputStream)
      
      // NOTE: IO on the "EDGE" of the app to help with pureness      
      // Display processed output
      packets.foreach{ packet =>
        println(packet)
        println(packet.header)
        packet.records.foreach(println)
      }
      
      // Display totals
      println("Total net flow packets processed = " + packets.size)
      println("Total flow records processed = " + packets.map(_.header.count).reduce(_+_))
    } 
  }
  
  def processFlowCapture(bufferedInputStream: BufferedInputStream): Vector[NetFlowPacket] = {
    val headerBytes = new Array[Byte](24)
    var packets: Vector[NetFlowPacket] = Vector()
    
    def readNextPacketHeader(): Unit = bufferedInputStream.read(headerBytes) match {
      case -1 => println("zero bytes read")
      case n => {
        packets = packets :+ processNetFlowPacket(ByteBuffer.wrap(headerBytes), bufferedInputStream)  
        readNextPacketHeader()
      }
    }  
    readNextPacketHeader()
    
    packets
  }
  
  def processNetFlowPacket(headerBytes: ByteBuffer, bufferedInputStream: BufferedInputStream): NetFlowPacket = {
    val packetHeader = processHeaderBytes(headerBytes)
    val payloadBytes = new Array[Byte](packetHeader.payloadLength)
    
    bufferedInputStream.read(payloadBytes)
    val records: Array[NetFlowRecord] = processFlowRecords(ByteBuffer.wrap(payloadBytes), packetHeader.count)  
    //records.foreach(println)
    NetFlowPacket(packetHeader, records)
  }
  
  def processFlowRecords(wrappedBytes: ByteBuffer, recordCount: Int):Array[NetFlowRecord] = {
    val records = 
      for {
        _ <- 1 to recordCount
        record = processAFlowRecord(wrappedBytes)
      } yield record
      
    records.toArray
  }
  
  def processAFlowRecord(wrappedBytes: ByteBuffer): NetFlowRecord = {
    import java.lang.{Short, Integer}
    val srcaddr = processIPAddress(wrappedBytes)
    val dstaddr = processIPAddress(wrappedBytes)
    val nexthop = processIPAddress(wrappedBytes)
    
    val input = Short.toUnsignedInt(wrappedBytes.getShort)
    val output = Short.toUnsignedInt(wrappedBytes.getShort)
    val dPkts = Integer.toUnsignedLong(wrappedBytes.getInt)
    val dOctets = Integer.toUnsignedLong(wrappedBytes.getInt)
    val First = Integer.toUnsignedLong(wrappedBytes.getInt)
    val Last = Integer.toUnsignedLong(wrappedBytes.getInt)
    val srcport = Short.toUnsignedInt(wrappedBytes.getShort)
    val dstport = Short.toUnsignedInt(wrappedBytes.getShort)
    
    // throw out the mid record padding
    wrappedBytes.get
    
    val tcp_flags = wrappedBytes.get
    val prot = wrappedBytes.get
    val tos = wrappedBytes.get
    val src_as = Short.toUnsignedInt(wrappedBytes.getShort)
    val dst_as = Short.toUnsignedInt(wrappedBytes.getShort)
    val src_mask = wrappedBytes.get
    val dst_mask = wrappedBytes.get
    
    // throw out the flow record end padding
    wrappedBytes.getShort
    
    NetFlowRecord(
      srcaddr,
      dstaddr,
      nexthop,
      input,
      output,
      dPkts,
      dOctets,
      First,
      Last,
      srcport,
      dstport,
      tcp_flags,
      prot,
      tos,
      src_as,
      dst_as,
      src_mask,
      dst_mask)
  }
  
  def processIPAddress(wrappedBytes: ByteBuffer): IPAddress = {
    import java.lang.Byte
    val octet1 = Byte.toUnsignedInt(wrappedBytes.get)  
    val octet2 = Byte.toUnsignedInt(wrappedBytes.get)
    val octet3 = Byte.toUnsignedInt(wrappedBytes.get)
    val octet4 = Byte.toUnsignedInt(wrappedBytes.get)
    IPAddress(octet1, octet2, octet3, octet4)
  }
  
  def processHeaderBytes(wrappedBytes: ByteBuffer): NetFlowHeader = {
    import java.lang.{Integer, Short}
    
    val version = wrappedBytes.getShort
    val count = wrappedBytes.getShort
    val sysUpTime = Integer.toUnsignedLong(wrappedBytes.getInt)
    val unix_secs = Integer.toUnsignedLong(wrappedBytes.getInt)
    val unix_nsecs = Integer.toUnsignedLong(wrappedBytes.getInt)
    val flow_sequence = Integer.toUnsignedLong(wrappedBytes.getInt)
    val engine_type = wrappedBytes.get
    val engine_id = wrappedBytes.get
    val sampling_interval = Short.toUnsignedInt(wrappedBytes.getShort)
    
    NetFlowHeader(
      version, 
      count, 
      sysUpTime, 
      unix_secs, 
      unix_nsecs, 
      flow_sequence, 
      engine_type,
      engine_id, 
      sampling_interval)
  }
  
  processRawNetFlowCapture
}

case class NetFlowPacket(header: NetFlowHeader, records: Array[NetFlowRecord]) {
  override def toString() = "\n********** NetFlowPacket ************"
}
    
case class NetFlowHeader(
    version: Int,
    count: Int, 
    sysUpTime: Long, 
    unix_secs: Long,
    unix_nsecs: Long,
    flow_sequence: Long,
    engine_type: Byte,
    engine_id: Byte,
    sampling_interval: Int) { 
  
  def payloadLength = count * 48
  
  override def toString() = {
    "NetFlowHeader(" + 
    "version: " + version + 
    ", count: " + count + 
    ", sysUpTime: " + sysUpTime +
    ", unix_secs: " + unix_secs +
    ", unix_nsecs: " + unix_nsecs +
    ", flow_sequence: " + flow_sequence +
    ", engine_type: " + engine_type +
    ", engine_id: " + engine_id +
    ", sampling_interval: " + sampling_interval + ")"
  }
}

case class NetFlowRecord(
    srcaddr:   IPAddress,
    dstaddr:   IPAddress,
    nexthop:   IPAddress,
    input:     Int,
    output:    Int,
    dPkts:     Long,
    dOctets:   Long,
    First:     Long,
    Last:      Long,
    srcport:   Int,
    dstport:   Int,
    tcp_flags: Byte,
    prot:      Byte,
    tos:       Byte,
    src_as:    Int,
    dst_as:    Int,
    src_mask:  Byte,
    dst_mask:  Byte) {
  
  override def toString() = {
    "NetFlowRecord(" + 
    "srcaddr: " + srcaddr + 
    ", dstaddr: " + dstaddr + 
    ", nexthop: " + nexthop +
    ", input: " + input +
    ", output: " + output +
    ", dPkts: " + dPkts +
    ", dOctets: " + dOctets +
    ", First: " + First +
    ", Last: " + Last + 
    ", srcport: " + srcport + 
    ", dstport: " + dstport + 
    ", tcp_flags: " + tcp_flags + 
    ", prot: " + prot + 
    ", tos: " + tos + 
    ", src_as: " + src_as + 
    ", dst_as: " + dst_as + 
    ", src_mask: " + src_mask + 
    ", dst_mask: " + dst_mask + ")"
  }  
}

case class IPAddress (
    octet1: Int,
    octet2: Int,
    octet3: Int,
    octet4: Int) {
  override def toString = octet1 + "." + octet2 + "." + octet3 + "." + octet4
}