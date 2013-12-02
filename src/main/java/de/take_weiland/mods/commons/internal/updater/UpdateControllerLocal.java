package de.take_weiland.mods.commons.internal.updater;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import de.take_weiland.mods.commons.internal.CommonsModContainer;
import de.take_weiland.mods.commons.internal.updater.tasks.InstallUpdate;
import de.take_weiland.mods.commons.internal.updater.tasks.SearchUpdates;

public class UpdateControllerLocal extends AbstractUpdateController {

	static final List<String> INTERNAL_MODS = Arrays.asList("mcp", "forge", "fml", "minecraft");
	
	private static final String LOG_CHANNEL = "Sevens ModUpdater";
	public static final Logger LOGGER;
	
	static {
		FMLLog.makeLog(LOG_CHANNEL);
		LOGGER = Logger.getLogger(LOG_CHANNEL);
	}
	
	private final ExecutorService executor;

	public UpdateControllerLocal() {
		List<UpdatableMod> mods = Lists.transform(Loader.instance().getActiveModList(), new Function<ModContainer, UpdatableMod>() {
			@Override
			public UpdatableMod apply(ModContainer container) {
				try {
					if (INTERNAL_MODS.contains(container.getModId().toLowerCase())) {
						return new FMLInternalMod(container, UpdateControllerLocal.this);
					} else {
						return new ModsFolderMod(container, UpdateControllerLocal.this);
					}
				} catch (Throwable t) {
					LOGGER.severe("Unexpected exception during UpdateableMod parsing!");
					LOGGER.severe("ModID: " + container.getModId());
					t.printStackTrace();
					return null;
				}
			}
		});
		
		this.mods = Maps.uniqueIndex(Iterables.filter(mods, Predicates.notNull()), ID_RETRIEVER);
		
		executor = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder().setNameFormat("Sevens ModUpdater %d").build());
	}
	
	@Override
	public void searchForUpdates(UpdatableMod mod) {
		validate(mod);
		if (mod.transition(ModUpdateState.CHECKING)) {
			executor.execute(new SearchUpdates(mod));
		}
	}
	
	@Override
	public void update(UpdatableMod mod, ModVersion version) {
		validate(mod);
		if (mod.transition(ModUpdateState.DOWNLOADING)) {
			executor.execute(new InstallUpdate(mod, version));
		}
	}
	
	private void validate(UpdatableMod mod) {
		if (!mods.containsKey(mod.getModId())) { // check key here, its faster
			throw new IllegalArgumentException(String.format("Mod %s not valid for this UpdateController!", mod.getModId()));
		}
	}

	@Override
	public boolean restartMinecraft() {
		// inspired from http://java.dzone.com/articles/programmatically-restart-java
		
		final List<String> command = Lists.newArrayList();

		String javaBinary = System.getProperty("java.home") + "/bin/java";

		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			javaBinary += ".exe";
		}
		
		if (!new File(javaBinary).canExecute()) {
			return false;
		}

		// java binary
		command.add("\"" + javaBinary + "\"");

		// vm arguments
		List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

		// if it's the agent argument : we ignore it otherwise the
		// address of the old application and the new one will be in conflict
		command.addAll(Collections2.filter(vmArguments, new Predicate<String>() {

			@Override
			public boolean apply(String arg) {
				return !arg.contains("-agentlib");
			}
			
		}));
		
		// program main and program arguments
		String sunJavaCommand = System.getProperty("sun.java.command");
		if (sunJavaCommand == null) {
			return false;
		}

		Iterator<String> mainCommand = Splitter.on(' ').omitEmptyStrings().trimResults().split(sunJavaCommand).iterator();

		if (!mainCommand.hasNext()) {
			return false;
		}
		
		String mainCommandFirst = mainCommand.next();

		// program main is a jar
		if (mainCommandFirst.endsWith(".jar")) {
			// if it's a jar, add -jar mainJar
			command.add("-jar");
			command.add("\"" + new File(mainCommandFirst).getPath() + "\"");
		} else {
			// else it's a .class, add the classpath and mainClass
			command.add("-cp");
			command.add("\"" + System.getProperty("java.class.path") + "\"");
			command.add(mainCommandFirst);
		}

		// finally add program arguments
		Iterators.addAll(command, mainCommand);

		System.out.println(Joiner.on(' ').join(command));
		
		try {
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.inheritIO();
			builder.start();
		} catch (IOException e) {
			return false;
		}
		
//		RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
//		String jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
//		
//		if (System.getProperty("os.name").toLowerCase().contains("win")) {
//			jvm += ".exe";
//		}
//		
//		if (!new File(jvm).canExecute()) {
//			return false;
//		}
//		
//		ArrayList<String> cmd = Lists.newArrayList();
//		
//		cmd.add(jvm);
//		
//		cmd.add("-cp");
//		cmd.add(mxBean.getClassPath());
//		
//        cmd.addAll(Lists.transform(mxBean.getInputArguments(), new Function<String, String>() {
//
//			@Override
//			public String apply(String input) {
//				input = input.trim();
//				if (!input.startsWith("-")) {
//					input = "-" + input;
//				}
//				return input;
//			}
//		}));
//        
//        String sunCommand = System.getProperty("sun.java.command");
//        
//        if (sunCommand == null) {
//        	return false;
//        }
//        
//        Iterables.addAll(cmd, Splitter.on(' ').omitEmptyStrings().trimResults().split(sunCommand));
//        
//        System.out.println(Joiner.on(' ').join(cmd));
//        try {
//			ProcessBuilder builder = new ProcessBuilder(cmd);
//        	builder.inheritIO();
//			builder.start();
//		} catch (IOException e) {
//			return false;
//		}
        		
		CommonsModContainer.proxy.shutdownMinecraft();
		return true;
	}
	
	
	
}