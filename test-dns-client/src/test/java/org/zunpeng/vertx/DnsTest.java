// package org.zunpeng.vertx;
//
// import com.qiniu.android.dns.DnsManager;
// import com.qiniu.android.dns.IResolver;
// import com.qiniu.android.dns.NetworkInfo;
// import com.qiniu.android.dns.local.Resolver;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
// import org.junit.jupiter.api.RepeatedTest;
// import org.junit.jupiter.api.Test;
//
// import java.net.InetAddress;
// import java.util.Arrays;
//
// public class DnsTest {
//
//   private static final Logger logger = LogManager.getLogger(DnsTest.class);
//
//   @Test
//   @RepeatedTest(50)
//   public void demo() {
//     try {
//       logger.info("------------- start");
//       IResolver[] resolvers = new IResolver[2];
//       resolvers[0] = new Resolver(InetAddress.getByName("119.29.29.29"), 1);
//       resolvers[1] = new Resolver(InetAddress.getByName("114.114.114.114"), 1);
//       // resolvers[0] = new Resolver(InetAddress.getByName("119.39.23.23"), 1);
//       // resolvers[1] = new Resolver(InetAddress.getByName("115.39.23.23"), 1);
//       DnsManager dns = new DnsManager(NetworkInfo.normal, resolvers);
//       String[] ips = dns.query("admin.baidu.com");
//       logger.info("ips: {}", Arrays.toString(ips));
//     } catch (Throwable t) {
//       logger.error(t.getMessage(), t);
//     }
//   }
// }
