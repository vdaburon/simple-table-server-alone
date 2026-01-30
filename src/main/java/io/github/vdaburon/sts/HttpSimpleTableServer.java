/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.github.vdaburon.sts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class HttpSimpleTableServer extends NanoHTTPD implements KeyWaiter {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger( HttpSimpleTableServer.class.getPackage().getName() );

    public static final String STS_VERSION = "5.0";
    public static final String ROOT = "/sts/";
    public static final String ROOT2 = "/sts";
    public static final String URI_INITFILE = "INITFILE";
    public static final String URI_READ = "READ";
    public static final String URI_READMULTI = "READMULTI";
    public static final String URI_ADD = "ADD";
    public static final String URI_SAVE = "SAVE";
    public static final String URI_LENGTH = "LENGTH";
    public static final String URI_STATUS = "STATUS";
    public static final String URI_RESET = "RESET";
    public static final String URI_STOP = "STOP";
    public static final String URI_CONFIG = "CONFIG";
    public static final String URI_FIND = "FIND";
    
    public static final String PARM_FILENAME = "FILENAME";
    public static final String PARM_LINE = "LINE";
    public static final String PARM_READ_MODE = "READ_MODE";
    public static final String PARM_ADD_MODE = "ADD_MODE";
    public static final String PARM_UNIQUE = "UNIQUE";
    public static final String PARM_KEEP = "KEEP";
    public static final String PARM_ADD_TIMESTAMP = "ADD_TIMESTAMP";
    public static final String PARM_FIND_MODE= "FIND_MODE";

    public static final String PARM_READMULTI_NB_LINES = "NB_LINES";
    public static final String VAL_FIRST = "FIRST";
    public static final String VAL_LAST = "LAST";
    public static final String VAL_RANDOM = "RANDOM";
    public static final String VAL_TRUE = "TRUE";
    public static final String VAL_FALSE = "FALSE";

    public static final String VAL_FIND_STR_SUBSTRING = "SUBSTRING";
    public static final String VAL_FIND_STR_EQUALS = "EQUALS";
    public static final String VAL_FIND_REGEX_FIND = "REGEX_FIND";
    public static final String VAL_FIND_REGEX_MATCH = "REGEX_MATCH";

    /* Program parameters end with _OPT */
    public static final String K_PORT_OPT = "port";
    public static final String K_DATASET_DIRECTORY_OPT = "datasetDirectory";
    public static final String K_ADD_TIMESTAMP_OPT = "addTimestamp";
    public static final String K_INIT_FILE_AT_STARTUP_OPT = "initFileAtStartup";
    public static final String K_INIT_FILE_AT_STARTUP_REGEX_OPT = "initFileAtStartupRegex";
    public static final String K_CHARSET_ENCODING_HTTP_REPONSE_OPT = "charsetEncodingHttpResponse";
    public static final String K_CHARSET_ENCODING_READ_FILE_OPT = "charsetEncodingReadFile";
    public static final String K_CHARSET_ENCODING_WRITE_FILE_OPT = "charsetEncodingWriteFile";
    public static final String K_IS_DEMON_OPT = "daemon";
    public static final String K_LEVEL_TRACE_OPT = "traceLevel";
    public static final String K_STOP_AND_EXIT_OPT = "stopExit";

    public static final int DEFAULT_PORT = 9191;
    public static final String DEFAULT_DATA_DIR = System.getProperty("user.dir");
    public static final boolean DEFAULT_TIMESTAMP = true;
    public static final boolean DEFAULT_DAEMON = false;
    public static final String DEFAULT_CHARSET_ENCODING_HTTP_RESPONSE = "UTF-8";
    public static final String DEFAULT_CHARSET_ENCODING_READ_FILE = "UTF-8";
    public static final String DEFAULT_CHARSET_ENCODING_WRITE_FILE = "UTF-8";
    public static final boolean DEFAULT_STOP_AND_EXIT = true;

    public static final String INDEX = 
            "<html><head><title>Help URL for the dataset</title><head>"
            + "<body><h4>Help Http Simple Table Server (STS) Alone Version : " + STS_VERSION + "<br/>From a data file (default: &lt;System.getProperty(\"user.dir\")&gt;) or from the directory set with cli argument datasetDirectory</h4>"
            + """
            <p>The parameter enclosed in square brackets is <b>[optional]</b> and and the values in italics correspond to the <b><i>possible values</i></b> <br />
            <p><b>Load file in memory:</b><br />
            http://hostname:port/sts/INITFILE?FILENAME=file.txt</p>
            <p><b>Get one line from list:</b><br />
            http://hostname:port/sts/READ?FILENAME=file.txt&[READ_MODE=[<i>FIRST</i> (Default),<i>LAST</i>,<i>RANDOM</i>]]&[KEEP=[<i>TRUE</i> (Default),<i>FALSE</i>]]</p>
            <p><b>Get multi lines from list in one request:</b><br />
            http://hostname:port/sts/READMULTI?FILENAME=file.txt&NB_LINES=number of lines to read : Integer &ge; 1 and &le; list size&[READ_MODE=[<i>FIRST</i> (default),<i>LAST</i>,<i>RANDOM</i>]]&[KEEP=[<i>TRUE</i> (Default),<i>FALSE</i>]]<br />
            E.g: http://hostname:port/sts/READMULTI?FILENAME=myfile.txt&NB_LINES=4&READ_MODE=FIRST&KEEP=TRUE<br /></p>
            <p><b>Find a line in a file:</b> (GET OR POST HTTP protocol)<br />The line to find is a string this SUBSTRING (Default) or EQUALS and a regular expression with REGEX_FIND (contains) and REGEX_MATCH (entire region the pattern).<br />
            GET : http://hostname:port/sts/FIND?FILENAME=file.txt&LINE=(BLUE|RED)&[FIND_MODE=[<i>SUBSTRING</i>,<i>EQUALS</i>,<i>REGEX_FIND</i>,<i>REGEX_MATCH</i>]]&[KEEP=[<i>TRUE</i>,<i>FALSE</i>]]<br />
            GET Parameters : FILENAME=file.txt&LINE=RED&[FIND_MODE=[<i>SUBSTRING</i>,<i>EQUALS</i>,<i>REGEX_FIND</i>,<i>REGEX_MATCH</i>]]&[KEEP=[<i>TRUE</i>,<i>FALSE</i>]]<br />
            <br />POST : http://hostname:port/sts/FIND<br />
            POST Parameters : FILENAME=file.txt,LINE=(BLUE|RED) or LINE=BLUE or LINE=B.* or LINE=.*E.* ,[FIND_MODE=[<i>SUBSTRING</i>,<i>EQUALS</i>,<i>REGEX_FIND</i>,<i>REGEX_MATCH</i>]]&[KEEP=[<i>TRUE</i>,<i>FALSE</i>]]<br />
            If NOT find return title KO and "Error : Not find !"</p>
            <p><b>Return the number of remaining lines of a linked list:</b><br/>
            http://hostname:port/sts/LENGTH?FILENAME=file.txt</p>
            <p><b>Add a line into a file:</b> (GET OR POST HTTP protocol)<br/>
            GET : http://hostname:port/sts/ADD?FILENAME=file.txt&LINE=D0001123&[ADD_MODE=[<i>FIRST</i>,<i>LAST</i>]]&[UNIQUE=[<i>FALSE</i>,<i>TRUE</i>]]<br />
            GET Parameters : FILENAME=file.txt&LINE=D0001123&[ADD_MODE=[<i>FIRST</i>,<i>LAST</i>]]&[UNIQUE=[<i>FALSE</i>,<i>TRUE</i>]]<br />
            <br />POST : http://hostname:port/sts/ADD<br />
            POST Parameters : FILENAME=file.txt,LINE=D0001123,[ADD_MODE=[<i>FIRST</i>,<i>LAST</i>]],[UNIQUE=[<i>FALSE</i>,<i>TRUE</i>]]</p>
            <p><b>Save the specified linked list in a file</b> to the default location:<br />
            http://hostname:port/sts/SAVE?FILENAME=file.txt&[ADD_TIMESTAMP=[<i>FALSE</i>,<i>TRUE</i>]]</p>
            <p><b>Display the list of loaded files and the number of remaining lines for each linked list:</b> <br />
            http://hostname:port/sts/STATUS</p>
            <p><b>Remove all of the elements from the specified list:</b> <br/>
            http://hostname:port/sts/RESET?FILENAME=file.txt</p>
            <p><b>Show configuration:</b><br />
            http://hostname:port/sts/CONFIG</p>
            <p><b>Shutdown the Simple Table Server:</b><br/>
            http://hostname:port/sts/STOP</p></body></html>
            <p><b>CLI Arguments:</b><br/>
            <p><b>Mode daemon :</b><br />
            -daemon [<i>true,false</i>] if <i>true</i> daemon process don't wait keyboards key pressed for nohup command, if <i>false</i> (default) wait keyboards key &lt;ENTER&gt; to Stop<br/>
            <h4>To load files at STS Startup use (optional) :</h4>
            Default values : initFileAtStartup="" (empty) and  initFileAtStartupRegex=false<br/>
            <p>E.g: read 3 csv files with comma separator (not a regular expression), files are read from the directory set with datasetDirectory <br/>
            -initFileAtStartup "file1.csv,file2.csv,file3.csv"<br/>
            -initFileAtStartupRegex false<br/>
            <p>OR<br />E.g: read all files with .csv extension declare with a regular expression (initFileAtStartupRegex=true) from directory set with datasetDirectory <br/>
            -initFileAtStartup ".+\\.csv"<br/>
            -initFileAtStartupRegex true<br/>
            <h4>Set the Charset to read, write file and send http response :</h4>
            <p>E.g : charset = UTF-8, ISO8859_15 or Cp1252 (Windows)<br/>
            -charsetEncodingHttpResponse &lt;charset&gt; (Use UTF-8) in the http header add "Content-Type:text/html; charset=&lt;charset&gt;", default UTF-8<br/>
            -charsetEncodingReadFile &lt;charset&gt; (set the charset corresponding to characters in the file), default System property : file.encoding<br/>
            -charsetEncodingWriteFile &lt;charset&gt; default System property : file.encoding<br/>
            <h4>Stop and Exit :</h4>
            -stopExit true, STOP command stops the STS server and call System.exit(0), default true<br/>
            -stopExit false, STOP command stops the STS server and don't call System.exit(0)<br/>
            </body></html>
            """;

    // CRLF ou LF ou CR
    public static String lineSeparator = System.lineSeparator();

    Map<String, LinkedList<String>> database = new HashMap<>();

    int port = DEFAULT_PORT;
    String dataDirectory = DEFAULT_DATA_DIR;
    boolean bTimestamp = DEFAULT_TIMESTAMP;
    Random myRandom = new Random();
    String charsetEncodingHttpResponse = DEFAULT_CHARSET_ENCODING_HTTP_RESPONSE;
    String charsetEncodingReadFile = DEFAULT_CHARSET_ENCODING_READ_FILE;
    String charsetEncodingWriteFile = DEFAULT_CHARSET_ENCODING_WRITE_FILE;
    boolean bIsDemon = DEFAULT_DAEMON;
    String initFileAtStartup = "";
    boolean isInitFileAtStartupRegex = false;
    boolean isStopAndExit = DEFAULT_STOP_AND_EXIT;

    public static void main(String[] args) {
        LOG.setLevel(java.util.logging.Level.INFO);

        Options options = createOptions();
        Properties parseProperties = null;

        try {
            parseProperties = parseOption(options, args);
        } catch (ParseException ex) {
            helpUsage(options);
            System.exit(1);
        }

        String sTmp;
        sTmp = (String) parseProperties.get(K_LEVEL_TRACE_OPT);
        if (sTmp != null) {
            if (sTmp.equalsIgnoreCase("WARN")) {
                LOG.setLevel(java.util.logging.Level.WARNING);
            }

            if (sTmp.equalsIgnoreCase("INFO")) {
                LOG.setLevel(java.util.logging.Level.INFO);
            }

            if (sTmp.equalsIgnoreCase("DEBUG")) {
                LOG.setLevel(java.util.logging.Level.FINE);
            }
        }

        LOG.info("parseProperties = " + parseProperties);

        int tempPort = DEFAULT_PORT;
        sTmp = (String) parseProperties.get(K_PORT_OPT);
        if (sTmp != null) {
            try {
                tempPort = Integer.parseInt(sTmp);
            } catch (NumberFormatException ex) {
                LOG.warning("Error parsing port value : " + sTmp + ", set port to default value : " + DEFAULT_PORT);
                tempPort = DEFAULT_PORT;
            }
        }

        HttpSimpleTableServer serv = new HttpSimpleTableServer(tempPort);
        serv.setPort(tempPort);

        sTmp = (String) parseProperties.get(K_DATASET_DIRECTORY_OPT);
        if (sTmp != null) {
            File fDir = new File(sTmp);
            if (!fDir.isDirectory()) {
                try {
                    String dirCanonical = fDir.getCanonicalPath();
                    LOG.warning("The " + K_DATASET_DIRECTORY_OPT + " value is not a directory : " + sTmp + ", canonical path : " + dirCanonical + ", set " + K_DATASET_DIRECTORY_OPT + " to default value : " + DEFAULT_DATA_DIR);
                } catch (IOException e) {
                    // Can't get the CanonicalPath from fDir, still use default path
                    LOG.warning("The " + K_DATASET_DIRECTORY_OPT + " value is not a directory : " + sTmp + ", set " + K_DATASET_DIRECTORY_OPT + " to default value : " + DEFAULT_DATA_DIR);
                }
            } else {
                serv.setDataDirectory(sTmp);
            }
        }

        sTmp = (String) parseProperties.get(K_ADD_TIMESTAMP_OPT);
        if (sTmp != null) {
            serv.setTimestamp(Boolean.parseBoolean(sTmp));
        }

        sTmp = (String) parseProperties.get(K_INIT_FILE_AT_STARTUP_OPT);
        if (sTmp != null) {
            serv.setInitFileAtStartup(sTmp);
        }

        sTmp = (String) parseProperties.get(K_INIT_FILE_AT_STARTUP_REGEX_OPT);
        if (sTmp != null) {
            serv.setInitFileAtStartupRegex(Boolean.parseBoolean(sTmp));
        }

        sTmp = (String) parseProperties.get(K_CHARSET_ENCODING_HTTP_REPONSE_OPT);
        if (sTmp != null) {
            serv.setCharsetEncodingHttpResponse(sTmp);
        }

        sTmp = (String) parseProperties.get(K_CHARSET_ENCODING_READ_FILE_OPT);
        if (sTmp != null) {
            serv.setCharsetEncodingReadFile(sTmp);
        }

        sTmp = (String) parseProperties.get(K_CHARSET_ENCODING_WRITE_FILE_OPT);
        if (sTmp != null) {
            serv.setCharsetEncodingWriteFile(sTmp);
        }

        sTmp = (String) parseProperties.get(K_IS_DEMON_OPT);
        if (sTmp != null) {
            serv.setDemon(Boolean.parseBoolean(sTmp));
        }

        sTmp = (String) parseProperties.get(K_STOP_AND_EXIT_OPT);
        if (sTmp != null) {
            serv.setStopExit(Boolean.parseBoolean(sTmp));
        }

        String dataDirectoryCanonical = "Can't get the dataDirectory canonicalPath";
        try {
            dataDirectoryCanonical = new File(serv.getDataDirectory()).getCanonicalPath();
        } catch (IOException e) {
            LOG.warning(dataDirectoryCanonical);
        }

        LOG.info("Creating HttpSimpleTable Alone");
        LOG.info("------------------------------");
        LOG.info(K_PORT_OPT + " : " + serv.getPort());
        LOG.info(K_DATASET_DIRECTORY_OPT + " : " + serv.getDataDirectory());
        LOG.info(K_DATASET_DIRECTORY_OPT + " (canonical) : " + dataDirectoryCanonical);
        LOG.info(K_ADD_TIMESTAMP_OPT + " : " + serv.isTimestamp());
        LOG.info(K_IS_DEMON_OPT + " : " + serv.isDemon());
        LOG.info(K_CHARSET_ENCODING_HTTP_REPONSE_OPT + " : " + serv.getCharsetEncodingHttpResponse());
        LOG.info(K_CHARSET_ENCODING_READ_FILE_OPT + " : " + serv.getCharsetEncodingReadFile());
        LOG.info(K_CHARSET_ENCODING_WRITE_FILE_OPT + " : " + serv.getCharsetEncodingWriteFile());
        LOG.info(K_INIT_FILE_AT_STARTUP_OPT + " : " + serv.getInitFileAtStartup());
        LOG.info(K_INIT_FILE_AT_STARTUP_REGEX_OPT + " : " + serv.isInitFileAtStartupRegex());
        LOG.info("------------------------------");
        LOG.info("STS_VERSION : " + STS_VERSION);
        ServerRunner.executeInstance(serv);
        try {
            serv.initFileAtStartup();
        } catch (IOException e) {
            LOG.warning("Some trouble when read files, initFileAtStartup = " + serv.getInitFileAtStartup());
        }

    }

    protected HttpSimpleTableServer(int port) {
    	 super(port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        String msg = "<html><title>KO</title>" + lineSeparator
                + "<body>Error : unknown command !</body>" + lineSeparator
                + "</html>";

        Map<String, String> files = new HashMap<>();
        if (Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR,
                        MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: "
                        + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT,
                        re.getMessage());
            }
        }
        Map<String, String> parms = session.getParms();
        if (uri.equals(ROOT) || uri.equals(ROOT2)) {
            msg = INDEX;
        } else {
            msg = doAction(uri, method, parms);
        }

        Response response = new Response(msg);

        // no cache for the response
        response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.addHeader("Pragma", "no-cache");
        response.addHeader("Expires", "0");
        // add the encoding charset
        response.addHeader("Content-Type", "text/html; charset=" +getCharsetEncodingHttpResponse());
        return response;
    }

    protected synchronized String doAction(String uri, Method method,
                                           Map<String, String> parms) {
        String msg = "<html><title>KO</title>" + lineSeparator
                + "<body>Error : unknown command !</body>" + lineSeparator
                + "</html>";
        if (uri.equals(ROOT + URI_INITFILE)) {
            msg = initFile(parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_READ)) {
            msg = read(parms.get(PARM_READ_MODE), parms.get(PARM_KEEP),
                    parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_READMULTI)) {
            msg = readmulti(parms.get(PARM_READ_MODE), parms.get(PARM_KEEP),
                    parms.get(PARM_FILENAME), parms.get(PARM_READMULTI_NB_LINES));
        }
        if (uri.equals(ROOT + URI_FIND) && (Method.POST.equals(method) || (Method.GET.equals(method)))) {
            msg = find(parms.get(PARM_FIND_MODE), parms.get(PARM_LINE), parms.get(PARM_KEEP),
                    parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_ADD) && (Method.POST.equals(method) || (Method.GET.equals(method)))) {
            msg = add(parms.get(PARM_ADD_MODE), parms.get(PARM_LINE),
                    parms.get(PARM_UNIQUE), parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_LENGTH)) {
            msg = length(parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_SAVE)) {
            msg = save(parms.get(PARM_FILENAME), parms.get(PARM_ADD_TIMESTAMP));
        }
        if (uri.equals(ROOT + URI_STATUS)) {
            msg = status();
        }
        if (uri.equals(ROOT + URI_RESET)) {
            msg = reset(parms.get(PARM_FILENAME));
        }
        if (uri.equals(ROOT + URI_CONFIG)) {
            msg = showConfig();
        }
        if (uri.equals(ROOT + URI_STOP)) {
            stopServer();
        }
        
        return msg;
    }

    private String status() {
        if (database.isEmpty()) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Database was empty !</body>"
                    + lineSeparator + "</html>";
        }
        String msg = "";
        for (String key : database.keySet()) {
            msg += key + " = " + database.get(key).size() + "<br />"
                    + lineSeparator;
        }
        return "<html><title>OK</title>" + lineSeparator + "<body>"
                + lineSeparator + msg + "</body></html>";
    }

    private String read(String readMode, String keepMode, String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            return "<html><title>KO</title>" + lineSeparator + "<body>Error : "
                    + filename + " not loaded yet !</body>" + lineSeparator
                    + "</html>";
        }
        if (database.get(filename).isEmpty()) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : No more line !</body>" + lineSeparator
                    + "</html>";
        }
        if (null == readMode) {
            readMode = VAL_FIRST;
        }
        if (null == keepMode) {
            keepMode = VAL_TRUE;
        }
        if (!VAL_FIRST.equals(readMode) && !VAL_LAST.equals(readMode)
                && !VAL_RANDOM.equals(readMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : READ_MODE value has to be FIRST, LAST or RANDOM !</body>"
                    + lineSeparator + "</html>";
        }
        if (!VAL_TRUE.equals(keepMode) && !VAL_FALSE.equals(keepMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : KEEP value has to be TRUE or FALSE !</body>"
                    + lineSeparator + "</html>";
        }
        String line;
        int index = 0;

        if (VAL_LAST.equals(readMode)) {
            index = database.get(filename).size() - 1;
        }
        if (VAL_RANDOM.equals(readMode)) {
            index = myRandom.nextInt(database.get(filename).size());
        }

        line = database.get(filename).remove(index);

        if (VAL_TRUE.equals(keepMode)) {
            database.get(filename).add(line);
        }
        return "<html><title>OK</title>" + lineSeparator + "<body>" + line
                + "</body>" + lineSeparator + "</html>";
    }

    private String readmulti(String readMode, String keepMode, String filename, String nbLinesToRead) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            return "<html><title>KO</title>" + lineSeparator + "<body>Error : "
                    + filename + " not loaded yet !</body>" + lineSeparator
                    + "</html>";
        }
        if (database.get(filename).isEmpty()) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : No more line !</body>" + lineSeparator
                    + "</html>";
        }
        if (null == readMode) {
            readMode = VAL_FIRST;
        }
        if (null == keepMode) {
            keepMode = VAL_TRUE;
        }
        if (!VAL_FIRST.equals(readMode) && !VAL_LAST.equals(readMode)
                && !VAL_RANDOM.equals(readMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : READ_MODE value has to be FIRST, LAST or RANDOM !</body>"
                    + lineSeparator + "</html>";
        }
        if (!VAL_TRUE.equals(keepMode) && !VAL_FALSE.equals(keepMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : KEEP value has to be TRUE or FALSE !</body>"
                    + lineSeparator + "</html>";
        }

        int nbLines = 1;
        try {
            nbLines = Integer.parseInt(nbLinesToRead);
        } catch (NumberFormatException ex) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : Can't parse integer parameter NB_LINES : " + nbLinesToRead + " !</body>"
                    + lineSeparator + "</html>";
        }

        if (nbLines <= 0) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : Parameter NB_LINES must be greater or equals than 1 : " + nbLinesToRead + " !</body>"
                    + lineSeparator + "</html>";
        }

        if (nbLines > database.get(filename).size()) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : Number lines to read greater than file size, " + nbLines + " greater than " +  database.get(filename).size() + " !</body>"
                    + lineSeparator + "</html>";
        }

        String line;
        int index = 0;

        String[] tabString = new String[nbLines];
        for (int i = 0; i < nbLines; i++) {
            if (VAL_LAST.equals(readMode)) {
                index = database.get(filename).size() - 1;
            }
            if (VAL_RANDOM.equals(readMode)) {
                index = myRandom.nextInt(database.get(filename).size());
            }

            line = database.get(filename).remove(index);
            tabString[i] = line;
        }

        // if keep add all lines at the end
        for (int i = 0; i < nbLines; i++) {
            if (VAL_TRUE.equals(keepMode)) {
                database.get(filename).add(tabString[i]);
            }
        }

        StringBuilder sb = new StringBuilder(2048);
        sb.append(lineSeparator);
        for (int i = 0; i < nbLines; i++) {
            sb.append(tabString[i]);
            sb.append("<br />");
            sb.append(lineSeparator);
        }

        return "<html><title>OK</title>" + lineSeparator + "<body>" + sb
                + "</body>" + lineSeparator + "</html>";
    }

    private String find(String findMode, String lineToFind, String keepMode, String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            return "<html><title>KO</title>" + lineSeparator + "<body>Error : "
                    + filename + " not loaded yet !</body>" + lineSeparator
                    + "</html>";
        }
        if (database.get(filename).isEmpty()) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : No more line !</body>" + lineSeparator
                    + "</html>";
        }

        if (lineToFind == null || lineToFind.isEmpty()) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Cant't find empty line !</body>" + lineSeparator
                    + "</html>";
        }

        if (null == findMode) {
            findMode = VAL_FIND_STR_SUBSTRING;
        }
        if (null == keepMode) {
            keepMode = VAL_TRUE;
        }
        if (!VAL_FIND_STR_SUBSTRING.equals(findMode) && !VAL_FIND_REGEX_FIND.equals(findMode) &&
            !VAL_FIND_STR_EQUALS.equals(findMode) && !VAL_FIND_REGEX_MATCH.equals(findMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : " + PARM_FIND_MODE + " value has to be " + VAL_FIND_STR_EQUALS + ", " + VAL_FIND_STR_SUBSTRING +
                    ", " +  VAL_FIND_REGEX_FIND + " or " + VAL_FIND_REGEX_MATCH + " !</body>"
                    + lineSeparator + "</html>";
        }
        if (!VAL_TRUE.equals(keepMode) && !VAL_FALSE.equals(keepMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : " + PARM_KEEP + " has to be TRUE or FALSE !</body>"
                    + lineSeparator + "</html>";
        }

        String line = "NOT_FOUND";
        int index = 0;
        boolean find = false;

        LinkedList<String> linkedList  = database.get(filename);

        if (VAL_FIND_STR_SUBSTRING.equals(findMode)) {
            Iterator<String> iterator = linkedList.iterator();
            while (iterator.hasNext() && !find) {
                String lineTmp = iterator.next();
                if (lineTmp.contains(lineToFind)) {
                    find = true;
                    line = lineTmp;
                } else {
                    index++;
                }
            }
        }

        if (VAL_FIND_STR_EQUALS.equals(findMode)) {
            Iterator<String> iterator = linkedList.iterator();
            while (iterator.hasNext() && !find) {
                String lineTmp = iterator.next();
                if (lineTmp.equals(lineToFind)) {
                    find = true;
                    line = lineTmp;
                } else {
                    index++;
                }
            }
        }

        if (VAL_FIND_REGEX_FIND.equals(findMode) || VAL_FIND_REGEX_MATCH.equals(findMode)) {
            Pattern p = null;
            try {
                p = Pattern.compile(lineToFind);
            } catch (PatternSyntaxException ex) {
                return "<html><title>KO</title>"
                        + lineSeparator
                        + "<body>Error : Regex compile error !</body>"
                        + lineSeparator + "</html>";
            }

            Iterator<String> iterator = linkedList.iterator();
            while (iterator.hasNext() && !find) {
                String lineTmp = iterator.next();
                Matcher m = p.matcher(lineTmp);
                if (VAL_FIND_REGEX_FIND.equals(findMode) && m.find()) {
                    find = true;
                    line = lineTmp;
                } else {
                    if (VAL_FIND_REGEX_MATCH.equals(findMode) && m.matches()) {
                        find = true;
                        line = lineTmp;
                    }
                    else {
                        index++;
                    }
                }
            }
        }

        if (!find) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Not find !</body>"
                    + lineSeparator + "</html>";
        }

        database.get(filename).remove(index);

        if (VAL_TRUE.equals(keepMode)) {
            database.get(filename).add(line);
        }
        return "<html><title>OK</title>" + lineSeparator + "<body>" + line
                + "</body>" + lineSeparator + "</html>";
    }

    private String add(String addMode, String line, String uniqueMode, String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (null == line) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : LINE parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            database.put(filename, new LinkedList<String>());
        }
        if (null == addMode) {
            addMode = VAL_FIRST;
        }
        if (null == uniqueMode) {
            uniqueMode = VAL_FALSE;
        }
        if (!VAL_FIRST.equals(addMode) && !VAL_LAST.equals(addMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : ADD_MODE value has to be FIRST or LAST !</body>"
                    + lineSeparator + "</html>";
        }
        if (!VAL_TRUE.equals(uniqueMode) && !VAL_FALSE.equals(uniqueMode)) {
            return "<html><title>KO</title>"
                    + lineSeparator
                    + "<body>Error : UNIQUE value has to be TRUE or FALSE !</body>"
                    + lineSeparator + "</html>";
        }

        if (VAL_TRUE.equals(uniqueMode)) {
            if (database.get(filename).contains(line)) {
                return "<html><title>KO</title>"
                        + lineSeparator
                        + "<body>Error : ENTRY already exists !</body>"
                        + lineSeparator + "</html>";
            }
        }

        if (VAL_FIRST.equals(addMode)) {
            database.get(filename).addFirst(line);
        } else {
            database.get(filename).add(line);
        }

        return "<html><title>OK</title>" + lineSeparator + "<body></body>"
                + lineSeparator + "</html>";
    }

    private String save(String filename, String paramAddTimeStamp) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (filename.matches(".*[\\\\/:].*") || filename.equals(".")
                || filename.equals("..")) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Illegal character found !</body>"
                    + lineSeparator + "</html>";
        }
        if (filename.length() > 128) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Maximum size reached (128) !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : LinkedList not found !</body>"
                    + lineSeparator + "</html>";
        }
        BufferedWriter out = null;
        String saveFilename = filename;
        boolean bParamAddTimestamp = bTimestamp;
        if (paramAddTimeStamp != null) {
        	bParamAddTimestamp = Boolean.parseBoolean(paramAddTimeStamp);
        }
        if (bParamAddTimestamp) {
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat(
                    "yyyyMMdd'T'HH'h'mm'm'ss's.'");
            saveFilename = ft.format(dNow) + filename;
        }
        try {
            Iterator<String> it = database.get(filename).iterator();
            // add the charset to write the file
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(getDataDirectory(),saveFilename)), getCharsetEncodingWriteFile()));
            while (it.hasNext()) {
                out.write(it.next());
                out.write(lineSeparator);
            }
        } catch (IOException e2) {
            return "<html><title>KO</title>" + lineSeparator + "<body>Error : "
                    + e2.getMessage() + "</body>" + lineSeparator + "</html>";
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException e3) {
                    return "<html><title>KO</title>" + lineSeparator
                            + "<body>Error : " + e3.getMessage() + "</body>"
                            + lineSeparator + "</html>";
                }
            }
        }
        return "<html><title>OK</title>" + lineSeparator + "<body>"
                + database.get(filename).size() + "</body>" + lineSeparator
                + "</html>";
    }

    private String length(String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (!database.containsKey(filename)) {
            return "<html><title>KO</title>" + lineSeparator + "<body>Error : "
                    + filename + " not loaded yet !</body>" + lineSeparator
                    + "</html>";
        }
        return "<html><title>OK</title>" + lineSeparator + "<body>"
                + database.get(filename).size() + "</body>" + lineSeparator
                + "</html>";
    }

    private String reset(String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (database.containsKey(filename)) {
            database.get(filename).clear();
        }
        return "<html><title>OK</title>" + lineSeparator + "<body></body>"
                + lineSeparator + "</html>";
    }

    private String initFile(String filename) {
        if (null == filename) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : FILENAME parameter was missing !</body>"
                    + lineSeparator + "</html>";
        }
        if (filename.matches(".*[\\\\/:].*") || filename.equals(".")
                || filename.equals("..")) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Illegal character found !</body>"
                    + lineSeparator + "</html>";
        }
        if (filename.length() > 128) {
            return "<html><title>KO</title>" + lineSeparator
                    + "<body>Error : Maximum size reached (128) !</body>"
                    + lineSeparator + "</html>";
        }

        LinkedList<String> lines = new LinkedList<String>();
        BufferedReader bufferReader = null;
        File f = new File(getDataDirectory(), filename);
        if (f.exists()) {
            try {
                // add the charset to read the file
            	bufferReader = new BufferedReader(new InputStreamReader(new FileInputStream(f),getCharsetEncodingReadFile()), 50 * 1024);
                String line;
                while ((line = bufferReader.readLine()) != null) {
                    lines.add(line);
                }
            } catch (FileNotFoundException e1) {
                return "<html><title>KO</title>" + lineSeparator
                        + "<body>Error : " + e1.getMessage() + "</body>"
                        + lineSeparator + "</html>";
            } catch (IOException e2) {
                return "<html><title>KO</title>" + lineSeparator
                        + "<body>Error : " + e2.getMessage() + "</body>"
                        + lineSeparator + "</html>";
            } finally {
                if (null != bufferReader) {
                    try {
                        bufferReader.close();
                    } catch (IOException e3) {
                        return "<html><title>KO</title>" + lineSeparator
                                + "<body>Error : " + e3.getMessage()
                                + "</body>" + lineSeparator + "</html>";
                    }
                }
            }
            database.put(filename, lines);
            return "<html><title>OK</title>" + lineSeparator + "<body>"
                    + lines.size() + "</body>" + lineSeparator + "</html>";
        }
        return "<html><title>KO</title>" + lineSeparator
                + "<body>Error : file not found !</body>" + lineSeparator
                + "</html>";
    }
    
    private String showConfig() {
        String dataDirectoryCanonical = "Can't get the dataDirectory canonicalPath";
        try {
            dataDirectoryCanonical = new File(getDataDirectory()).getCanonicalPath();
        } catch (IOException e) {
            LOG.warning(dataDirectoryCanonical);
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append(K_PORT_OPT + "=" + getPort() + "<br />" + lineSeparator);
        sb.append(K_DATASET_DIRECTORY_OPT + "=" + getDataDirectory() + "<br />" + lineSeparator);
        sb.append(K_DATASET_DIRECTORY_OPT + " (canonical)=" + dataDirectoryCanonical + "<br />" + lineSeparator);
        sb.append(K_ADD_TIMESTAMP_OPT + "=" + isTimestamp() + "<br />" + lineSeparator);
        sb.append(K_IS_DEMON_OPT + "=" + isDemon() + "<br />" + lineSeparator);
        sb.append(K_CHARSET_ENCODING_HTTP_REPONSE_OPT + "=" + getCharsetEncodingHttpResponse() + "<br />" + lineSeparator);
        sb.append(K_CHARSET_ENCODING_READ_FILE_OPT + "=" + getCharsetEncodingReadFile() + "<br />" + lineSeparator);
        sb.append(K_CHARSET_ENCODING_WRITE_FILE_OPT + "=" + getCharsetEncodingWriteFile() + "<br />" + lineSeparator);
        sb.append(K_INIT_FILE_AT_STARTUP_OPT + "=" + getInitFileAtStartup() + "<br />" + lineSeparator);
        sb.append(K_INIT_FILE_AT_STARTUP_REGEX_OPT + "=" + isInitFileAtStartupRegex() + "<br />" + lineSeparator);
        sb.append(K_STOP_AND_EXIT_OPT + "=" + isStopExit() + "<br />" + lineSeparator);
        sb.append("databaseIsEmpty=" + database.isEmpty() + "<br />" + lineSeparator);
        return "<html><title>OK</title>" + lineSeparator + "<body>"
                + lineSeparator + sb.toString() + "</body></html>";
    }

    public void stopServer() {
        LOG.info("HTTP Simple Table Server is shutting down...");
        stop();

        if (isStopExit()) {
            LOG.info("... And Exit");
            System.exit(0);
        }
    }

    // only when start STS from Command Line Interface (script shell : simple-table-server.cmd or simple-table-server.sh)
    public void waitForKey() {
    	try {
    		// load files at STS startup
			initFileAtStartup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if (!bIsDemon) {
            LOG.warning("Hit Enter Key on keyboards to Stop and Exit");
	        try {
	            System.in.read();
	        } catch (Throwable ignored) {
	        }
    	} else { // mode daemon
            LOG.warning("Mode daemon process, call 'http://hostname:port/sts/STOP' or kill this process to end the Http Simple Table Server");
    		boolean infiniteLoop = true;
    		while (infiniteLoop) {
    			try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
                    LOG.warning("Daemon process killed");
				}
    		}
    	}
    }
    
    public void initFileAtStartup() throws IOException {
        final String sInitFileAtStartup = getInitFileAtStartup();
        boolean bInitFileAtStartupRegex = isInitFileAtStartupRegex();
        if (sInitFileAtStartup.length() > 0) {
            LOG.info("INITFILE at STS startup");
            LOG.info("initFileAtStartup=" + sInitFileAtStartup);
            LOG.info("initFileAtStartupRegex=" + bInitFileAtStartupRegex);
        }
        if (sInitFileAtStartup.length() > 0 && bInitFileAtStartupRegex == false) {
            // E.g : initFileAtStartup=file1.csv,file2.csv,file3.csv
           
            String[] tabFileName = sInitFileAtStartup.split(",");
            for (int i = 0; i < tabFileName.length; i++) {
                String fileName = tabFileName[i].trim();
                LOG.info("INITFILE : i = " + i + ", fileName = " + fileName);
                String fileNameUrlEncoded =  URLEncoder.encode(fileName,StandardCharsets.UTF_8);
                if (!fileName.equals(fileNameUrlEncoded)) {
                    LOG.info("fileNameUrlEncoded=<" + fileNameUrlEncoded + ">");
                }
                URL url = new URL("http://localhost:" + port +"/sts/INITFILE?FILENAME=" + fileNameUrlEncoded);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        		String inputLine;
        		StringBuffer content = new StringBuffer();
        		while ((inputLine = in.readLine()) != null) {
        		    content.append(inputLine);
        		}
        		in.close();
        		con.disconnect();

                LOG.info(url.toString() + ", response=" + content);
            }
        }
        
        if (sInitFileAtStartup.length() > 0 && bInitFileAtStartupRegex == true) {
            // E.g : initFileAtStartup=file\d+\.csv regex match : file1.csv, file2.csv, file3.csv, file44.csv ...
           
            String dataDir = getDataDirectory(); // default System.getProperty("user.dir")
            File fDir = new File(dataDir);
            File [] files = fDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.matches(sInitFileAtStartup);
                }
            });
        
            for (int i = 0; i < files.length; i++) {
                String fileName = files[i].getName();
                LOG.info("INITFILE : i = " + i + ", fileName = " + fileName);
                String fileNameUrlEncoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                if (!fileName.equals(fileNameUrlEncoded)) {
                    LOG.info("fileNameUrlEncoded=<" + fileNameUrlEncoded + ">");
                }
                URL url = new URL("http://localhost:" + port +"/sts/INITFILE?FILENAME=" + fileNameUrlEncoded);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        		String inputLine;
        		StringBuilder content = new StringBuilder();
        		while ((inputLine = in.readLine()) != null) {
        		    content.append(inputLine);
        		}
        		in.close();
        		con.disconnect();

                LOG.info(url.toString() + ", response=" + content);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public boolean isTimestamp() {
        return bTimestamp;
    }

    public void setTimestamp(boolean bTimestamp) {
        this.bTimestamp = bTimestamp;
    }

    public String getCharsetEncodingHttpResponse() {
        return charsetEncodingHttpResponse;
    }

    public void setCharsetEncodingHttpResponse(String charsetEncodingHttpResponse) {
        this.charsetEncodingHttpResponse = charsetEncodingHttpResponse;
    }

    public String getCharsetEncodingReadFile() {
        return charsetEncodingReadFile;
    }

    public void setCharsetEncodingReadFile(String charsetEncodingReadFile) {
        this.charsetEncodingReadFile = charsetEncodingReadFile;
    }

    public String getCharsetEncodingWriteFile() {
        return charsetEncodingWriteFile;
    }

    public void setCharsetEncodingWriteFile(String charsetEncodingWriteFile) {
        this.charsetEncodingWriteFile = charsetEncodingWriteFile;
    }

    public boolean isDemon() {
        return bIsDemon;
    }

    public void setDemon(boolean bIsDemon) {
        this.bIsDemon = bIsDemon;
    }

    public String getInitFileAtStartup() {
        return initFileAtStartup;
    }

    public void setInitFileAtStartup(String initFileAtStartup) {
        this.initFileAtStartup = initFileAtStartup;
    }

    public boolean isInitFileAtStartupRegex() {
        return isInitFileAtStartupRegex;
    }

    public void setInitFileAtStartupRegex(boolean initFileAtStartupRegex) {
        isInitFileAtStartupRegex = initFileAtStartupRegex;
    }

    public boolean isStopExit() {
        return isStopAndExit;
    }

    public void setStopExit(boolean stopExit) {
        isStopAndExit = stopExit;
    }

    private static Options createOptions() {
        Options options = new Options();

        Option helpOpt = Option.builder("help").hasArg(false).desc("Help and show parameters").build();

        options.addOption(helpOpt);

        Option portOpt = Option.builder(K_PORT_OPT).argName(K_PORT_OPT)
                .hasArg(true)
                .required(false)
                .desc("Listening port (default 9191)")
                .build();
        options.addOption(portOpt);

        Option datasetDirectoryOpt = Option.builder(K_DATASET_DIRECTORY_OPT).argName(K_DATASET_DIRECTORY_OPT)
                .hasArg(true)
                .required(false)
                .desc("Directory where csv files are read or save (default : current directory where this tool is launch)")
                .build();
        options.addOption(datasetDirectoryOpt);


        Option addTimestampOpt = Option.builder(K_ADD_TIMESTAMP_OPT).argName(K_ADD_TIMESTAMP_OPT)
                .hasArg(true)
                .required(false)
                .desc("Add timestamp prefix when save file")
                .build();
        options.addOption(addTimestampOpt);

        Option initFileAtStartupOpt = Option.builder(K_INIT_FILE_AT_STARTUP_OPT).argName(K_INIT_FILE_AT_STARTUP_OPT)
                .hasArg(true)
                .required(false).desc("Files to read at startup")
                .build();
        options.addOption(initFileAtStartupOpt);

        Option initFileAtStartupRegexOpt = Option.builder(K_INIT_FILE_AT_STARTUP_REGEX_OPT).argName(K_INIT_FILE_AT_STARTUP_REGEX_OPT)
                .hasArg(true)
                .required(false)
                .desc("Is the liste files to read is a regular expression (true) or a list of files (false) ? default false")
                .build();
        options.addOption(initFileAtStartupRegexOpt);

        Option charsetEncodingHttpResponseOpt = Option.builder(K_CHARSET_ENCODING_HTTP_REPONSE_OPT).argName(K_CHARSET_ENCODING_HTTP_REPONSE_OPT)
                .hasArg(true)
                .required(false)
                .desc("In the http header add \"Content-Type:text/html; charset=<charsetEncoding>\", default (UTF-8)")
                .build();
        options.addOption(charsetEncodingHttpResponseOpt);

        Option charsetEncodingReadFileOpt = Option.builder(K_CHARSET_ENCODING_READ_FILE_OPT).argName(K_CHARSET_ENCODING_READ_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Charset to read the file in memory")
                .build();
        options.addOption(charsetEncodingReadFileOpt);

        Option charsetEncodingWriteFileOpt = Option.builder(K_CHARSET_ENCODING_WRITE_FILE_OPT).argName(K_CHARSET_ENCODING_WRITE_FILE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Charset to write (save) the file")
                .build();
        options.addOption(charsetEncodingWriteFileOpt);

        Option niveauTraceOpt = Option.builder(K_LEVEL_TRACE_OPT).argName(K_LEVEL_TRACE_OPT)
                .hasArg(true)
                .required(false)
                .desc("Trace level WARN, INFO (default), DEBUG").build();
        options.addOption(niveauTraceOpt);

        Option stsDemonOpt = Option.builder(K_IS_DEMON_OPT).argName(K_IS_DEMON_OPT)
                .hasArg(true)
                .required(false).
                desc("If true daemon process don't wait keyboards key pressed for nohup command, if false (default) wait keyboards key <ENTER> to Stop")
                .build();
        options.addOption(stsDemonOpt);

        Option stsStopAndExitOpt = Option.builder(K_STOP_AND_EXIT_OPT).argName(K_STOP_AND_EXIT_OPT)
                .hasArg(true)
                .required(false).
                desc("If true STOP command stops STS and call System.exit(0), if false STOP command stops STS Server but don't call exit, default true")
                .build();
        options.addOption(stsStopAndExitOpt);

        return options;
    }

    private static Properties parseOption(Options optionsP, String[] args) throws ParseException {
        Properties properties = new Properties();

        CommandLineParser parser = new DefaultParser();
        // parse the command line arguments
        CommandLine line = parser.parse(optionsP, args);

        if (line.hasOption("help")) {
            properties.setProperty("help", "help value");
            return properties;
        }

        if (line.hasOption(K_PORT_OPT)) {
            properties.setProperty(K_PORT_OPT, line.getOptionValue(K_PORT_OPT));
        }

        if (line.hasOption(K_DATASET_DIRECTORY_OPT)) {
            properties.setProperty(K_DATASET_DIRECTORY_OPT, line.getOptionValue(K_DATASET_DIRECTORY_OPT));
        }

        if (line.hasOption(K_ADD_TIMESTAMP_OPT)) {
            properties.setProperty(K_ADD_TIMESTAMP_OPT, line.getOptionValue(K_ADD_TIMESTAMP_OPT));
        }

        if (line.hasOption(K_INIT_FILE_AT_STARTUP_OPT)) {
            properties.setProperty(K_INIT_FILE_AT_STARTUP_OPT, line.getOptionValue(K_INIT_FILE_AT_STARTUP_OPT));
        }

        if (line.hasOption(K_INIT_FILE_AT_STARTUP_REGEX_OPT)) {
            properties.setProperty(K_INIT_FILE_AT_STARTUP_REGEX_OPT, line.getOptionValue(K_INIT_FILE_AT_STARTUP_REGEX_OPT));
        }

        if (line.hasOption(K_CHARSET_ENCODING_HTTP_REPONSE_OPT)) {
            properties.setProperty(K_CHARSET_ENCODING_HTTP_REPONSE_OPT, line.getOptionValue(K_CHARSET_ENCODING_HTTP_REPONSE_OPT));
        }

        if (line.hasOption(K_CHARSET_ENCODING_READ_FILE_OPT)) {
            properties.setProperty(K_CHARSET_ENCODING_READ_FILE_OPT, line.getOptionValue(K_CHARSET_ENCODING_READ_FILE_OPT));
        }

        if (line.hasOption(K_CHARSET_ENCODING_WRITE_FILE_OPT)) {
            properties.setProperty(K_CHARSET_ENCODING_WRITE_FILE_OPT, line.getOptionValue(K_CHARSET_ENCODING_WRITE_FILE_OPT));
        }

        if (line.hasOption(K_CHARSET_ENCODING_WRITE_FILE_OPT)) {
            properties.setProperty(K_CHARSET_ENCODING_WRITE_FILE_OPT, line.getOptionValue(K_CHARSET_ENCODING_WRITE_FILE_OPT));
        }

        if (line.hasOption(K_IS_DEMON_OPT)) {
            properties.setProperty(K_IS_DEMON_OPT, line.getOptionValue(K_IS_DEMON_OPT));
        }

        if (line.hasOption(K_STOP_AND_EXIT_OPT)) {
            properties.setProperty(K_STOP_AND_EXIT_OPT, line.getOptionValue(K_STOP_AND_EXIT_OPT));
        }
        return properties;
    }

    private static void helpUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        String footer = "E.g: java -jar simple-table-server-alone-<version>-jar-with-dependencies.jar -" + K_PORT_OPT + " 9191 -"
                + K_DATASET_DIRECTORY_OPT + " \"/sts/data\" -" + K_ADD_TIMESTAMP_OPT + " true -" + K_IS_DEMON_OPT + " true";
        formatter.printHelp(120, HttpSimpleTableServer.class.getName(),
                HttpSimpleTableServer.class.getName(), options, footer, true);
    }
}