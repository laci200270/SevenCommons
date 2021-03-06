package de.take_weiland.mods.commons.crash;

import de.take_weiland.mods.commons.util.JavaUtils;
import de.take_weiland.mods.commons.util.Scheduler;
import de.take_weiland.mods.commons.util.Sides;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;

/**
* @author diesieben07
*/
enum CrashExceptionHandler implements Thread.UncaughtExceptionHandler {

	INSTANCE;

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		final ReportedException re;
		if (e instanceof ReportedException) {
			re = (ReportedException) e;
		} else {
			CrashReport cr = new CrashReport("Uncaught Exception in Thread", e);
			CrashReportCategory cat = cr.makeCategory("Thread in Question");
			cat.addCrashSection("Thread name", t.getName());
			cat.addCrashSection("Thread ID", t.getId());
			cat.addCrashSection("Daemon?", t.isDaemon());
			cat.addCrashSection("Thread Status", t.getState());
			re = new ReportedException(cr);
		}
		Scheduler.forSide(Sides.environment()).execute(new Runnable() {
			@Override
			public void run() {
				throw JavaUtils.throwUnchecked(re);
			}
		});
	}
}
