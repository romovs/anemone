package haven;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DbgUtils {

	public static long getCpuTime() {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0L;
	}

}
