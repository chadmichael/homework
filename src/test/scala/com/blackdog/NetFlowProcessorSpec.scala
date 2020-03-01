package com.blackdog

import org.scalatest._
import FlowCaptureProcessor._
import java.nio.ByteBuffer


class NetFlowProcessorSpec extends FlatSpec with Matchers {  
  //coerce the higher unsigned values into a single byte 
  implicit def int2Byte(i: Int) = i.toByte
  
  "FlowCaptureProcessor.processIPAddress" should "process raw header bytes into an IPAddress" in {
    val wrappedBytes: ByteBuffer = ByteBuffer.wrap(Array[Byte](
        0xA8,
        0x01,
        0x03,
        0xD0))
        
        val ipAddress: IPAddress = FlowCaptureProcessor.processIPAddress(wrappedBytes);
    
        assert(ipAddress.octet1 == 168)
        assert(ipAddress.octet2 == 1)
        assert(ipAddress.octet3 == 3)
        assert(ipAddress.octet4 == 208)
        assert(ipAddress.toString() == "168.1.3.208")
  }

  "FlowCaptureProcessor.processHeaderBytes" should "process raw header bytes into NetFlowHeader" in {    
    val wrappedBytes: ByteBuffer = ByteBuffer.wrap(Array[Byte](
        0x00, 
        0x05, 
        0x00, 
        0x18, 
        0x0B, 
        0x77, 
        0x90, 
        0x30, 
        0x3D, 
        0xE9, 
        0xFC, 
        0xFB, 
        0x32, 
        0x91, 
        0x2E, 
        0x14, 
        0x00, 
        0x00, 
        0x00, 
        0x01, 
        0x00, 
        0x00,
        0x00, 
        0x00))
        
    val header = FlowCaptureProcessor.processHeaderBytes(wrappedBytes)
    
    assert(header.version == 5)
    assert(header.count == 24)
    assert(header.unix_nsecs == 848375316)
    assert(header.unix_secs == 1038744827)
    assert(header.sysUpTime == 192385072)
    assert(header.engine_id == 0)
    assert(header.engine_type == 0)
    assert(header.flow_sequence == 1)
    assert(header.sampling_interval == 0)        
  }
  
  "FlowCaptureProcessor.processAFlowRecord" should "process raw record bytes into a NetFlowRecord" in { 
    val wrappedBytes: ByteBuffer = ByteBuffer.wrap(Array[Byte](
      0xC0, 
      0xA8, 
      0x01, 
      0x03, 
      0xD0, 
      0x55, 
      0x28, 
      0x14, 
      0x0A, 
      0x01, 
      0x00, 
      0x01, 
      0x00, 
      0x02, 
      0x00, 
      0x01, 
      0x00, 
      0x00, 
      0x00, 
      0x21, 
      0x00, 
      0x00,
      0x1B, 
      0x1B, 
      0x0B, 
      0x76, 
      0xFC, 
      0xE8, 
      0x0B, 
      0x77, 
      0x2A, 
      0x54, 
      0x06,
      0x30, 
      0x00, 
      0x50, 
      0x00, 
      0x1A, 
      0x06,
      0x00, 
      0x00,
      0x00,
      0x00,
      0x00,
      0x18,
      0x00,
      0x00,
      0x00))
     
      val aRecord = FlowCaptureProcessor.processFlowRecordBytes(wrappedBytes)
      
      assert(aRecord.dOctets == 6939)
      assert(aRecord.dPkts == 33)
      assert(aRecord.dst_as == 0)
      assert(aRecord.dst_mask == 0)
      assert(aRecord.dstaddr.toString == "208.85.40.20")
      assert(aRecord.dstport == 80)
      assert(aRecord.First == 192347368)
      assert(aRecord.Last == 192358996)
      assert(aRecord.input == 2)
      assert(aRecord.nexthop.toString  == "10.1.0.1")
      assert(aRecord.output == 1)
      assert(aRecord.prot == 6)
      assert(aRecord.src_as == 0)
      assert(aRecord.src_mask == 24)
      assert(aRecord.srcaddr.toString  == "192.168.1.3")
      assert(aRecord.srcport == 1584)
      assert(aRecord.tcp_flags == 26)
      assert(aRecord.tos == 0)
  }
}