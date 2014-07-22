/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.jso.devmode.launcher;

import org.apache.commons.cli.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.teavm.jso.JS;
import org.teavm.jso.devmode.JSAwareClassLoader;
import org.teavm.jso.devmode.JSRemoteEndpoint;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DevmodeLauncher {
    public int port;
    public Class<?> mainClass;

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(OptionBuilder
                .withArgName("port number")
                .hasArg()
                .withDescription("a colon-separated package list which to transform")
                .withLongOpt("package")
                .create('k'));
        options.addOption(OptionBuilder
                .withArgName("number")
                .hasArg()
                .withDescription("port of DevMode server. Default is 8888")
                .withLongOpt("port")
                .create('p'));

        if (args.length == 0) {
            printUsage(options);
            return;
        }

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage(options);
            return;
        }

        int port = 8888;
        if (commandLine.hasOption('p')) {
            try {
                port = Integer.parseInt(commandLine.getOptionValue('p'));
            } catch (NumberFormatException e) {
                printUsage(options);
                return;
            }
        }
        String[] packages = new String[0];
        if (commandLine.hasOption('k')) {
            packages = commandLine.getOptionValue('k').split(":");
        }
        args = commandLine.getArgs();
        if (args.length != 1) {
            printUsage(options);
        }

        JSAwareClassLoader classLoader = new JSAwareClassLoader(DevmodeLauncher.class.getClassLoader());
        for (String pkg : packages) {
            classLoader.addPackageToInstrument(pkg);
        }
        classLoader.addPackageToInstrument(DevmodeLauncher.class.getPackage().getName());
        Class<?> mainClass = Class.forName(args[0], true, classLoader);
        Class<?> launcherCls = Class.forName(DevmodeLauncher.class.getName(), true, classLoader);
        Object launcher = launcherCls.newInstance();
        launcherCls.getField("port").set(launcher, port);
        launcherCls.getField("mainClass").set(launcher, mainClass);
        launcherCls.getMethod("launch").invoke(launcher);
    }

    public void launch() throws Exception {
        JS.getGlobal();

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        JSRemoteEndpoint.setMainClass(mainClass);
        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
        wscontainer.addEndpoint(JSRemoteEndpoint.class);
        server.start();
        server.dump(System.err);
        server.join();
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + DevmodeLauncher.class.getName() + " [OPTIONS] [qualified.main.Class]", options);
        System.exit(-1);
    }
}
