package org.yao.jute;

import com.google.common.base.MoreObjects;
import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.apache.jute.Index;
import org.apache.jute.InputArchive;
import org.apache.jute.OutputArchive;
import org.apache.jute.Record;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.TreeMap;

/** BinaryOutputArchive and BinaryInputArchive ignore the <tt>tag</tt> argument in all methods. */
public class JuteTests {
  private String pathname = "jute-data";

  @Test
  public void testSerDe() throws Exception {
    Files.deleteIfExists(Paths.get(pathname));
    serialize();
    deserialize();
  }

  private void serialize() throws Exception {
    try (OutputStream os = new FileOutputStream(new File(pathname)); ) {
      BinaryOutputArchive oa = BinaryOutputArchive.getArchive(os);

      // Primitive types
      oa.writeBool(true, "boolean");
      oa.writeInt(1024, "int");
      oa.writeString("yao", "string");

      // Records
      Student xiaoMing = new Student(2, "xiaoMing");
      oa.writeRecord(xiaoMing, "xiaoMing");

      // TreeMap
      TreeMap<String, Integer> map = new TreeMap<>();
      map.put("one", 1);
      map.put("two", 2);
      oa.startMap(map, "map");
      int i = 1;
      for (String key : map.keySet()) {
        String tag = i + "";
        oa.writeString(key, tag);
        oa.writeInt(map.get(key), tag);
        i++;
      }
      oa.endMap(map, "map");
    }
  }

  private void deserialize() throws Exception {
    try (FileInputStream is = new FileInputStream(new File(pathname)); ) {
      BinaryInputArchive ia = BinaryInputArchive.getArchive(is);
      System.out.printf("boolean: %b\n", ia.readBool("boolean"));
      System.out.printf("int: %d\n", ia.readInt("int"));
      System.out.printf("string: %s\n", ia.readString("string"));

      Student xiaoMing = new Student();
      ia.readRecord(xiaoMing, "xiaoMing");
      System.out.printf("xiaoMing: %s\n", xiaoMing);

      Index index = ia.startMap("map");
      int i = 1;
      while (!index.done()) {
        String tag = i + "";
        System.out.printf("key: %s, value: %d\n", ia.readString(tag), ia.readInt(tag));
        index.incr();
        i++;
      }
    }
  }
}

class Student implements Record {
  private int grade;
  private String name;

  public Student() {}

  public Student(int grade, String name) {
    this.grade = grade;
    this.name = name;
  }

  @Override
  public void serialize(OutputArchive oa, String tag) throws IOException {
    oa.startRecord(this, tag);
    oa.writeInt(grade, "grade");
    oa.writeString(name, "name");
    oa.endRecord(this, tag);
  }

  @Override
  public void deserialize(InputArchive ia, String tag) throws IOException {
    ia.startRecord(tag);
    grade = ia.readInt("grade");
    name = ia.readString("name");
    ia.endRecord(tag);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("grade", grade).add("name", name).toString();
  }
}
