package org.exoplatform.selenium;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Transforms the Selenium IDE recorded html to a corresponding JUnit class.
 */
public class SeleniumTestCaseGenerator {

	private static final String TEST_PATTERN = "Test_";

	private static int testsNumber = 0;
	private static int testsSuitesNumber = 0;

	private String basedir;
	private String outputdir;

	public static void main(String[] args) throws Exception {
		SeleniumTestCaseGenerator seleneseToJavaBuilder = null;
		if (args.length == 1) {
			seleneseToJavaBuilder = new SeleniumTestCaseGenerator(args[0], args[0]);
		} else if (args.length == 2) {
			seleneseToJavaBuilder = new SeleniumTestCaseGenerator(args[0], args[1]);
		} else {
			throw new IllegalArgumentException("SeleneseToJavaBuilder suitePath [Target] [TestSpeed]");
		}
		seleneseToJavaBuilder.run();
		System.out
		      .println("Done : " + testsNumber + " tests generated, " + testsSuitesNumber + " tests suites generated");
	}

	public SeleniumTestCaseGenerator(String basedir, String outputdir) throws Exception {
		this.basedir = basedir;
		this.outputdir = outputdir;
	}

	public void run() throws Exception {
		generate(new File(basedir), "");
	}

	public void generate(File directoryFile, String path) throws Exception {
		File[] files = directoryFile.listFiles();
		String testPackagePath = path.replaceAll(replaceSeparatorPattern, ".");
		for (int j = 0; j < files.length; j++) {
			File file = files[j];
			String filePath = path.length()>0 ? path + File.separator + file.getName() : file.getName();
			if (file.isDirectory() && !file.getName().startsWith(".")) {
				generate(new File(directoryFile, file.getName()), filePath);
			} else if (file.getName().endsWith(".html") && file.getName().startsWith(TEST_PATTERN)) {
				generateFile(filePath, testPackagePath);
			}
		}
	}

	public void generateFile(String seleniumFile, String testPackagePath) throws Exception {
		seleniumFile = seleniumFile.replaceAll(replaceSeparatorPattern, "/");

		int x = seleniumFile.lastIndexOf("/");
		int y = seleniumFile.indexOf(".");
		String testName = seleniumFile.substring(x + 1, y);
		String testMethodName = "test" + testName.substring(5);
		String testFileName = outputdir + "/" + seleniumFile.substring(0, y) + ".java";

		// Write each Test in one file
		StringBuffer sb = new StringBuffer();
		sb.append("package " + testPackagePath + ";\n\n");
		// sb.append("import org.exoplatform.util.selenium.BaseTestCase;\n");
		sb.append("import junit.framework.TestCase;\n");
                sb.append("import java.io.File;\n");
                sb.append("import org.apache.commons.io.FileUtils;\n");

		sb.append("import com.thoughtworks.selenium.*;\n");
		sb.append("public class " + testName + " extends SeleneseTestCase {\n");

		// setSpeed & setUp
		appendCommonMethods(sb);

		// testMethod
		appendTest(sb, seleniumFile, testName, testMethodName);
		sb.append("}\n");

		String content = sb.toString();
		writeFile(testFileName, content);

		testsNumber++;
	}

	private void appendCommonMethods(StringBuffer sb) {
		sb.append("public String speed = \"100\";\n");
		sb.append("public String timeout = \"30000\";\n");
		sb.append("public int timeoutSecInt = 30;\n");
		sb.append("public String browser = \"firefox\";\n");
                sb.append("public String host = \"localhost\";\n");
                sb.append("public String hostPort = \"8080\";\n");		
		sb.append("public void setSpeed() {\n  selenium.setSpeed(speed);\n}\n\n");
		sb.append("public void setUp() throws Exception {\n");
		sb.append("  browser = System.getProperty(\"selenium.browser\", browser);\n");
		sb.append("  timeout = System.getProperty(\"selenium.timeout\", timeout);\n");
		sb.append("  timeoutSecInt = Integer.parseInt(timeout)/1000;\n");		
		sb.append("  speed = System.getProperty(\"selenium.speed\", speed);\n");
                sb.append("  host = System.getProperty(\"selenium.host\", host);\n");
                sb.append("  hostPort = System.getProperty(\"selenium.host.port\", hostPort);\n");
		sb.append("  super.setUp(\"http://\" + host + \":\" + hostPort + \"/portal/\", \"*\" + browser);\n");
		sb.append("}\n\n");
	}

	private void appendTest(StringBuffer sb, String scriptFile, String testName, String testMethodName) throws Exception {

		String xml = FileUtils.readFileToString(new File(basedir + "/" + scriptFile), "UTF-8");

		System.out.println("* " + basedir + "/" + scriptFile);

		// Method
		sb.append("public void " + testMethodName + "() throws Exception {\n");
		sb.append("  setSpeed();\n");

		if ((xml.indexOf("<title>" + testName + "</title>") == -1)
		      || (xml.indexOf("colspan=\"3\">" + testName + "</td>") == -1)) {
			System.out.println("[WARN] The test name inside the file should be the file name.");
		}

		if (xml.indexOf("&quot;") != -1) {
			xml = replace(xml, "&quot;", "\"");
			//writeFile(outputdir + "/" + scriptFile, xml);
		}

		int x = xml.indexOf("<tbody>");
		int y = xml.indexOf("</tbody>");

		xml = xml.substring(x, y + 8);

		x = 0;
		y = 0;
		int count = 0;

		while (true) {
			x = xml.indexOf("<tr>", x);
			y = xml.indexOf("\n</tr>", x);
			if ((x == -1) || (y == -1)) {
				break;
			}

			x += 6;
			y++;
			count++;

			String step = xml.substring(x, y);
			String[] params = getParams(step);

			String param1 = params[0];
			String param2 = fixParam(params[1]);
//variables management
			param2 = param2.replaceAll("\\$\\{([a-z0-9A-Z]*)\\}", "\" + $1 + \"");
			param2 = param2.replaceAll("storedVars\\['([a-z0-9A-Z]*)'\\]", "'\" + $1 + \"'");
			String param3 = fixParam(params[2]);
//variables management
			param3 = param3.replaceAll("\\$\\{([a-z0-9A-Z]*)\\}", "\" + $1 + \"");
			param3 = param3.replaceAll("storedVars\\['([a-z0-9A-Z]*)'\\]", "'\" + $1 + \"'");

			sb.append("\n  //" + count + ": " + param1 + " | " + param2 + " | " + param3 + "\n");
			if (param1.equals("assertConfirmation") || param1.equals("verifyConfirmation")) {
				param2 = replace(param2, "?", "[\\\\s\\\\S]");
				sb.append("TestCase.assertTrue(selenium.getConfirmation().matches(\"^");
				sb.append(param2);
				sb.append("$\"));\n");
			} else if (param1.equals("assertAlert")) {
				sb.append("TestCase.assertTrue(selenium.getAlert().matches(\"^");
				sb.append(param2);
				sb.append("$\"));\n");
			}else if (param1.equals("assertLocation")) {
				sb.append("TestCase.assertTrue(selenium.getLocation().matches(\"^");
				sb.append(param2);
				sb.append("$\"));\n");
			}else if (param1.equals("waitForValue")) {
				sb.append("TestCase.assertTrue(selenium.getValue(\"");
				sb.append(param2);
				sb.append("\").matches(\"^");
				sb.append(param3);
				sb.append("$\"));\n");
			}else if (param1.equals("assertElementPresent") || param1.equals("assertElementNotPresent")) {
				if (param1.equals("assertElementPresent")) {
					sb.append("TestCase.assertTrue");
				} else if (param1.equals("assertElementNotPresent")) {
					sb.append("TestCase.assertFalse");
				}
				sb.append("(selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("assertTextPresent") || param1.equals("assertTextNotPresent")) {
				if (param1.equals("assertTextPresent")) {
					sb.append("TestCase.assertTrue");
				} else if (param1.equals("assertTextNotPresent")) {
					sb.append("TestCase.assertFalse");
				}
				sb.append("(selenium.isTextPresent(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("click") ||param1.equals("contextMenu") || param1.equals("mouseDown") || param1.equals("doubleClick") || param1.equals("mouseDownRight")|| param1.equals("mouseUp")
			      || param1.equals("open") || param1.equals("selectFrame") || param1.equals("selectWindow")|| param1.equals("focus")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");
			} else if (param1.equals("clickAndWait")) {
				sb.append("selenium.click(\"");
				sb.append(param2);
				sb.append("\");\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("clickAt") || param1.equals("mouseMoveAt")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"1,1\");\n");
			} else if (param1.equals("clickAtAndWait")) {
				sb.append("selenium.clickAt(\"");
				sb.append(param2);
				sb.append("\", \"1,1\");\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("close") || param1.equals("chooseCancelOnNextConfirmation")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("();\n");
			} else if (param1.equals("pause")) {
				sb.append("Thread.sleep(");
				sb.append(param2);
				sb.append(");\n");
			} else if (param1.equals("addSelection") || param1.equals("select") || param1.equals("type")
			      || param1.equals("typeKeys") || param1.equals("waitForPopUp")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
			} else if (param1.equals("selectAndWait")) {
				sb.append("selenium.select(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("storeTexttmp")) {
				sb.append("String ");
				sb.append(param3);
				sb.append(" = selenium.getText(\"");
				sb.append(param2);
				sb.append("\");\n");
				sb.append("RuntimeVariables.setValue(\"");
				sb.append(param3);
				sb.append("\", ");
				sb.append(param3);
				sb.append(");\n");
			} else if (param1.equals("verifyElementPresent") || param1.equals("verifyElementNotPresent")) {
				if (param1.equals("verifyElementPresent")) {
					sb.append("TestCase.assertTrue");
				} else if (param1.equals("verifyElementNotPresent")) {
					sb.append("TestCase.assertFalse");
				}
				sb.append("(selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("verifyTextPresent") || param1.equals("verifyTextNotPresent")) {
				if (param1.equals("verifyTextPresent")) {
					sb.append("TestCase.assertTrue");
				} else if (param1.equals("verifyTextNotPresent")) {
					sb.append("TestCase.assertFalse");
				}
				sb.append("(selenium.isTextPresent(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("verifyText")) {
				sb.append("TestCase.assertTrue");
				sb.append("(selenium.getText(\"");
				sb.append(param2);
				sb.append("\").equals(\"");
				sb.append(param3);
				sb.append("\"));\n");
			} else if (param1.equals("verifyTitle")) {
				sb.append("TestCase.assertEquals(\"");
				sb.append(param2);
				sb.append("\", selenium.getTitle());\n");
			} else if (param1.equals("waitForElementNotPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (!selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForElementPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n if (selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\")) \nbreak; }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForTextPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n if (selenium.isTextPresent(\"");
				sb.append(param2);
				sb.append("\")) \nbreak; }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForTextNotPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n if (!selenium.isTextPresent(\"");
				sb.append(param2);
				sb.append("\")) \nbreak; }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForTable")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n");
				sb.append("if (\"\".equals(selenium.getTable(\"");
				sb.append(param2);
				sb.append("\"))) {\nbreak;\n}\n}\ncatch (Exception e) {\n}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("mouseDownAt") || param1.equals("mouseUpAt")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\",\"");
				sb.append(param3);
				sb.append("\");\n");
			} else if (param1.equals("verifyValue")) {
				sb.append("TestCase.assertEquals(\"");
				sb.append(param3);
				sb.append("\", selenium.getValue(\"");
				sb.append(param2);
				sb.append("\"));\n");
                        } else if (param1.equals("waitForAlert")) {
				sb.append("waitForAlert(\"");
				sb.append(param2);
				sb.append("\");\n");
			} else if (param1.equals("waitForConfirmation")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n");
				sb.append("if (selenium.getConfirmation().equals(\"");
				sb.append(param2);
				sb.append("\")) {\nbreak;\n}\n}\ncatch (Exception e) {\n}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			}else if (param1.equals("waitForConfirmationPresent")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n");
				sb.append("if (selenium.isConfirmationPresent()");
				sb.append("){\nbreak;\n}\n}\ncatch (Exception e) {\n}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("verifyEval")) {
				sb.append("TestCase.assertEquals(\"");
				sb.append(param3);
				sb.append("\", selenium.getEval(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("mouseOver") || param1.equals("mouseOut")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\");\n");
			} else if (param1.equals("assertVisible")) {
				sb.append("TestCase.assertTrue(selenium.isVisible");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("verifyVisible")) {
			sb.append("TestCase.assertTrue(selenium.isVisible");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("verifyChecked")) {
				sb.append("verifyTrue(selenium.isChecked");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
                        } else if (param1.equals("waitForNotVisible")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (!selenium.isVisible(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
                        } else if (param1.equals("verifySelectedValue")) {
				sb.append("TestCase.assertTrue");
				sb.append("(selenium.getSelectedValue(\"");
				sb.append(param2);
				sb.append("\").equals(\"");
				sb.append(param3);
				sb.append("\"));\n");
                        } else if (param1.equals("verifyNotVisible")) {
				sb.append("TestCase.assertFalse(selenium.isVisible");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
                        } else if (param1.equals("waitForVisible")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (selenium.isVisible(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForChecked")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (selenium.isChecked(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForNotChecked")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (!selenium.isChecked(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForLocation")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\n");
				sb.append("if (selenium.getLocation().equals(\"");
				sb.append(param2);
				sb.append("\")) {\nbreak;\n}\n}\ncatch (Exception e) {\n}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("waitForSelectedValue")) {
				sb.append("TestCase.assertTrue(selenium.getSelectedValue(\"");
				sb.append(param2);
				sb.append("\").matches(\"^");
				sb.append(param3);
				sb.append("$\"));\n");
			} else if (param1.equals("waitForAttribute")) {
				sb.append("TestCase.assertTrue(selenium.getAttribute(\"");
				sb.append(param2);
				sb.append("\").matches(\"^");
				sb.append(param3);
				sb.append("$\"));\n");
			} else if (param1.equals("doubleClickAt")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"1,1\");\n");
			} else if (param1.equals("verifySelectedLabel")) {
				sb.append("TestCase.assertTrue");
				sb.append("(selenium.getSelectedLabel(\"");
				sb.append(param2);
				sb.append("\").equals(\"");
				sb.append(param3);
				sb.append("\"));\n");
			} else if (param1.equals("storeEval")) {
				sb.append("String ").append(param3).append(" = selenium.getEval(\"").append(param2).append("\").toString();\n");
			} else if (param1.equals("store")) {
				sb.append("String ").append(param3).append(" = (\"\" + ").append(param2).append(" + \"\").toString();\n");
			} else if (param1.equals("keyDown")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
			} else if (param1.equals("verifyAttribute")) {
				sb.append("TestCase.assertTrue");
				sb.append("(selenium.getAttribute(\"");
				sb.append(param2);
				sb.append("\").equals(\"");
				sb.append(param3);
				sb.append("\"));\n");
			} else if (param1.equals("assertNotVisible")) {
				sb.append("TestCase.assertFalse(selenium.isVisible");
				sb.append("(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("waitForNotSpeed")) {
				sb.append("selenium.waitForNotSpeed(\"");
				sb.append(param2);
				sb.append("\");\n");
			}  else if (param1.equals("waitForSpeed")) {
				sb.append("selenium.waitForSpeed(\"");
				sb.append(param2);
				sb.append("\");\n");			
                        } else if (param1.equals("assertValue")) {
				sb.append("TestCase.assertEquals(\"");
				sb.append(param3);
				sb.append("\", selenium.getValue(\"");
				sb.append(param2);
				sb.append("\"));\n");
			} else if (param1.equals("setSpeed")) {
				sb.append("selenium.setSpeed(\"").append(param2).append("\");\n");
			} else if (param1.equals("chooseOkOnNextConfirmation")) {
				sb.append("selenium.chooseOkOnNextConfirmation();\n");
			} else if (param1.equals("storeConfirmation")) {
				sb.append("String ").append(param2).append(" = selenium.getConfirmation();\n");
			} else if (param1.equals("keyPress")) {
				sb.append("selenium.keyPress(\"").append(param2).append("\",\"").append(param3).append("\");\n");
			} else if (param1.equals("check")) {
				sb.append("selenium.check(\"").append(param2).append("\");\n");
			} else if (param1.equals("uncheck")) {
				sb.append("selenium.uncheck(\"").append(param2).append("\");\n");
			} else if (param1.equals("verifyNotChecked")) {
				sb.append("verifyFalse(selenium.isChecked(\"").append(param2).append("\"));\n");
			} else if (param1.equals("deleteCookie")) {
				sb.append("selenium.deleteCookie(\"").append(param2).append("\",\"").append(param3).append("\");\n");
			} else if (param1.equals("windowMaximize")) {
				sb.append("selenium.windowMaximize()").append(";\n");
			} else if (param1.equals("waitForText")) {
				sb.append("for (int second = 0;; second++) {\n");
				sb.append(getTimeoutMessage(param1));
				sb.append("try {\nif (selenium.isElementPresent(\"");
				sb.append(param2);
				sb.append("\"))\n break;\n }\n catch (Exception e) {}\n");
				sb.append("Thread.sleep(1000);\n");
				sb.append("}\n");
			} else if (param1.equals("refresh")) {
				sb.append("selenium.refresh();\n");
			 
			//------------------User extension----------------
			/**
        		 * CreateFolderbyTime will create a Folder on Linux by time with current date and newest Rev : revXXXXX/YYYYMMDD/
			 * Account use to create Folder is root
			 */
			} else if (param1.equals("eXoCreateFolderReportPLF30x")) {
				sb.append("String pathDirReportPLF30x = \"/home/SELENIUM-PLF3.0.X/workspace/Commit_Result_PLF_3_0_6/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportPLF30x).mkdirs();\n");
			} else if (param1.equals("eXocopyReportPLF30x")) {
				sb.append("File fOrigPLF30x = new File(\"/home/SELENIUM-PLF3.0.X/workspace/Run_Selenium_on_PLF_3_0_6/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestPLF30x = new File(pathDirReportPLF30x);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigPLF30x, fDestPLF30x); \n");
			}else if (param1.equals("eXoCreateFolderReportPLF35x")) {
				sb.append("String pathDirReportPLF35x = \"/home/SELENIUM-PLF3.5.X/workspace/Commit_Result_PLF_3_5_0_CR1/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportPLF35x).mkdirs();\n");
			} else if (param1.equals("eXocopyReportPLF35x")) {
				sb.append("File fOrigPLF35x = new File(\"/home/SELENIUM-PLF3.5.X/workspace/Run_Selenium_on_PLF_3_5_0_CR1/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestPLF35x = new File(pathDirReportPLF35x);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigPLF35x, fDestPLF35x); \n");
                         } else if (param1.equals("createReportCS22X")) {
				sb.append("String pathDirReportCS22X = \"/home/SELENIUM-CS-CLIENT/workspace/Commit_Result_CS_2_2_4_SNAPSHOT/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportCS22X).mkdirs();\n");
			} else if (param1.equals("copyReportCS22X")) {
				sb.append("File fOrigCS22X = new File(\"/home/SELENIUM-CS-CLIENT/workspace/Run_Selenium_CS_2_2_4_SNAPSHOT/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestCS22X = new File(pathDirReportCS22X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigCS22X, fDestCS22X); \n");
			} else if (param1.equals("createReportCS21X")) {
				sb.append("String pathDirReportCS21X = \"/home/SELENIUM-CS-CLIENT/workspace/Commit_Result_CS_2_1_6/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportCS21X).mkdirs();\n");
			} else if (param1.equals("copyReportCS21X")) {
				sb.append("File fOrigCS21X = new File(\"/home/SELENIUM-CS-CLIENT/workspace/Run_Selenium_on_CS_2_1_6/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
					sb.append("File fDestCS21X = new File(pathDirReportCS21X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigCS21X, fDestCS21X); \n");
			} else if (param1.equals("createReport_CS230")) {
				sb.append("String pathDirReportCS = \"/home/SELENIUM-CS-CLIENT/workspace/Commit_Result_CS_2_3_0_SNAPSHOT/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportCS).mkdirs();\n");
			} else if (param1.equals("copyReport_CS230")) {
				sb.append("File fOrigCS = new File(\"/home/SELENIUM-CS-CLIENT/workspace/Run_Selenium_on_CS_2_3_0_SNAPSHOT/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestCS = new File(pathDirReportCS);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigCS, fDestCS); \n");
			} else if (param1.equals("createReportKS230")) {
				sb.append("String pathDirReportKS = \"/home/SELENIUM-KS-CLIENT/workspace/Commit_Result_KS_2_3_0_SNAPSHOT/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportKS).mkdirs();\n");
			} else if (param1.equals("copyReportKS230")) {
				sb.append("File fOrigKS = new File(\"/home/SELENIUM-KS-CLIENT/workspace/Run_Selenium_on_KS_2_3_0_SNAPSHOT/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestKS = new File(pathDirReportKS);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigKS, fDestKS); \n");
			} else if (param1.equals("createReportKS22X")) {
				sb.append("String pathDirReportKS22X = \"/home/SELENIUM-KS-CLIENT/workspace/Commit_Result_KS_2_2_2/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportKS22X).mkdirs();\n");
			} else if (param1.equals("copyReportKS22X")) {
				sb.append("File fOrigKS22X = new File(\"/home/SELENIUM-KS-CLIENT/workspace/Run_Selenium_on_KS_2_2_2/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestKS22X = new File(pathDirReportKS22X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigKS22X, fDestKS22X); \n");
			} else if (param1.equals("createReportKS21X")) {
				sb.append("String pathDirReportKS21X = \"/home/SELENIUM-KS-CLIENT/workspace/Commit_Result_KS_2_1_6/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportKS21X).mkdirs();\n");
			} else if (param1.equals("copyReportKS21X")) {
				sb.append("File fOrigKS21X = new File(\"/home/SELENIUM-KS-CLIENT/workspace/Run_Selenium_on_KS_2_1_6/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestKS21X = new File(pathDirReportKS21X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigKS21X, fDestKS21X); \n");
			} else if (param1.equals("createReportECMS230GA")) {
				sb.append("String pathDirReportECMS = \"/home/SELENIUM-ECMS-CLIENT/workspace/Commit_Result_ECMS_2_3_0_GA/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportECMS).mkdirs();\n");
			} else if (param1.equals("copyReportECMS230GA")) {
				sb.append("File fOrigECMS = new File(\"/home/SELENIUM-ECMS-CLIENT/workspace/Run_Selenium_on_ECMS_2_3_0_GA/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestECMS = new File(pathDirReportECMS);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigECMS, fDestECMS); \n");
			}else if (param1.equals("ReporteXoGTN320")) {
				sb.append("String pathDirReportGATEIN = \"/home/SELENIUM-GATEIN-CLIENT/workspace/Commit_Result_eXoGTN_3_2_0_PLF/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportGATEIN).mkdirs();\n");
			} else if (param1.equals("copyReporteXoGTN320")) {
				sb.append("File fOrigGATEIN = new File(\"/home/SELENIUM-GATEIN-CLIENT/workspace/Run_Selenium_eXoGTN_3_2_0_PLF/target/site/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestGATEIN = new File(pathDirReportGATEIN);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigGATEIN, fDestGATEIN); \n");
			} else if (param1.equals("ReporteXoWebOS")) {
				sb.append("String pathDirReportWebOS = \"/home/SELENIUM-GATEIN-CLIENT/workspace/Commit_Result_eXoWebOS_2_0_0/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportWebOS).mkdirs();\n");
			} else if (param1.equals("copyReporteXoWebOS")) {
				sb.append("File fOrigWebOS = new File(\"/home/SELENIUM-GATEIN-CLIENT/workspace/Run_Selenium_WebOS_2_0_0/target/site/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestWebOS = new File(pathDirReportWebOS);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigWebOS, fDestWebOS); \n");
			} else if (param1.equals("ReporteXoGTN3110")) {
				sb.append("String pathDirReportGTN3110 = \"/home/SELENIUM-GATEIN-CLIENT/workspace/Commit_Result_eXoGTN_3_1_10_PLF/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportGTN3110).mkdirs();\n");
			} else if (param1.equals("copyReporteXoGTN3110")) {
				sb.append("File fOrigGTN3110 = new File(\"/home/SELENIUM-GATEIN-CLIENT/workspace/Run_Selenium_eXoGTN_3_1_10_PLF/target/site/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestGTN3110 = new File(pathDirReportGTN3110);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigGTN3110, fDestGTN3110); \n");
			}else if (param1.equals("ReportJbossGTN")) {
				sb.append("String pathDirReportGTN3110 = \"/home/SELENIUM-GATEIN-CLIENT/workspace/Commit_Result_JBossGTN_3_2_0_M02/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportGTN3110).mkdirs();\n");
			} else if (param1.equals("copyReportJbossGTN")) {
				sb.append("File fOrigJbossGTN = new File(\"/home/SELENIUM-GATEIN-CLIENT/workspace/Run_Selenium_JBossGTN_3_2_0_M02/target/site/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestJbossGTN = new File(pathDirReportJbossGTN);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigJbossGTN, fDestJbossGTN); \n");
			} else if (param1.equals("eXoCreateFolderReportSOC")) {
				sb.append("String pathDirReportSOC = \"/home/SELENIUM-SOCIAL-CLIENT/workspace/Commit_Result_SOC_1_3_0_SNAPSHOT/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportSOC).mkdirs();\n");
			} else if (param1.equals("eXocopyReportSOC")) {
				sb.append("File fOrigSOC = new File(\"/home/SELENIUM-SOCIAL-CLIENT/workspace/Run_Selenium_on_SOC_1_3_0_SNAPSHOT/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestSOC = new File(pathDirReportSOC);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigSOC, fDestSOC); \n");
                        } else if (param1.equals("eXoCreateFolderReportSOC11X")) {
				sb.append("String pathDirReportSOC11X = \"/home/SELENIUM-SOCIAL-CLIENT/workspace/Commit_Result_SOC_1_1_6/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportSOC11X).mkdirs();\n");
			} else if (param1.equals("eXocopyReportSOC11X")) {
				sb.append("File fOrigSOC11X = new File(\"/home/SELENIUM-SOCIAL-CLIENT/workspace/Run_Selenium_on_SOC_1_1_6/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestSOC11X = new File(pathDirReportSOC11X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigSOC11X, fDestSOC11X); \n");
                        } else if (param1.equals("eXoCreateFolderReportSOC12X")) {
				sb.append("String pathDirReportSOC12X = \"/home/SELENIUM-SOCIAL-CLIENT/workspace/Commit_Result_SOC_1_2_3_SNAPSHOT/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportSOC12X).mkdirs();\n");
			} else if (param1.equals("eXocopyReportSOC12X")) {
				sb.append("File fOrigSOC12X = new File(\"/home/SELENIUM-SOCIAL-CLIENT/workspace/Run_Selenium_SOC_1_2_3_SNAPSHOT/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestSOC12X = new File(pathDirReportSOC12X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigSOC12X, fDestSOC12X); \n");
                       } else if (param1.equals("eXoCreateFolderReportWebOS")) {
				sb.append("String pathDirReportWebOS = \"/home/SELENIUM-GATEIN-CLIENT/workspace/Commit_Result_eXoWebOS_2_2_0/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportWebOS).mkdirs();\n");
                      } else if (param1.equals("eXocopyReportWebOS")) {
				sb.append("File fOrigWebOS = new File(\"/home/SELENIUM-GATEIN-CLIENT/workspace/Run_Selenium_WebOS_2_2_0/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestWebOS = new File(pathDirReportWebOS);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigWebOS, fDestWebOS); \n");
		     } else if (param1.equals("createReportECMS216")) {
				sb.append("String pathDirReportECMS21X = \"/home/SELENIUM-ECMS-CLIENT/workspace/Commit_Result_ECMS_2_1_6/rev\" + ");
                                sb.append(param3);
				sb.append(" + ");
				sb.append("\"/\"");
                                sb.append(" + ");
				sb.append(param2);
				sb.append("; \n");
				sb.append("new File(pathDirReportECMS21X).mkdirs();\n");
			} else if (param1.equals("copyReportECMS216")) {
				sb.append("File fOrigECMS21X = new File(\"/home/SELENIUM-ECMS-CLIENT/workspace/Run_Selenium_on_ECMS_2_1_6/target/tests.exoplatform.org/\"");
				sb.append(" + ");
                                sb.append(param2);	       
                                sb.append(");\n");
				sb.append("File fDestECMS21X = new File(pathDirReportECMS21X);\n");
                                sb.append("FileUtils.copyFileToDirectory(fOrigECMS21X, fDestECMS21X); \n");
    //---------------------------------end-----------
			} else if (param1.equals("keyDown")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
			} else if (param1.equals("keyUp")) {
				sb.append("selenium.");
				sb.append(param1);
				sb.append("(\"");
				sb.append(param2);
				sb.append("\", \"");
				sb.append(param3);
				sb.append("\");\n");
			} else if (param1.equals("storeValue")) {
				sb.append("String ");
				sb.append(param3);
				sb.append(" = selenium.getValue(\"");
				sb.append(param2);
				sb.append("\");\n");
				sb.append("RuntimeVariables.setValue(\"");
				sb.append(param3);
				sb.append("\", ");
				sb.append(param3);
				sb.append(");\n");
                        } else if (param1.equals("verifyLocation")) {
				sb.append("TestCase.assertEquals(\"");
              			sb.append(param2);
				sb.append("\", selenium.getLocation());\n");

                        } else if (param1.equals("TypeRandom")) {
				sb.append("selenium.getEval(\"selenium.doTypeRandom(\\\"");
                                sb.append(param2);
				sb.append("\\\",\\\"");
                                sb.append(param3);
				sb.append("\\\"));\n");
			} else if (param1.equals("refreshAndWait")) {
				sb.append("selenium.refresh();\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("storeXpathCount")) {
				sb.append("String ").append(param3).append(" = selenium.getXpathCount(\"").append(param2).append(
				      "\").toString();\n");
			}else if (param1.equals("storeText")) {
				sb.append("String ").append(param3).append(" = selenium.getText(\"").append(param2).append(
				      "\").toString();\n");
			} else if (param1.equals("verifyOrdered")) {
				sb.append("selenium.isOrdered(\"").append(param2).append("\",\"").append(param3).append("\");\n"); 
			} else if (param1.equals("dragAndDropToObject")) {
				sb.append("selenium.dragAndDropToObject(\"").append(param2).append("\",\"").append(param3).append("\");\n");


			} else if (param1.equals("componentExoContextMenu")) {
				sb.append("selenium.getEval(\"selenium.doComponentExoContextMenu(\\\"").append(param2)
				      .append("\\\")\");\n");
			} else if (param1.equals("componentExoContextMenuAndWait")) {
				sb.append("selenium.doComponentExoContextMenu(\"");
				sb.append(param2);
				sb.append("\");\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("getExoExtensionVersion")) {
				sb.append("selenium.getEval(\"selenium.doGetExoExtensionVersion(\\\"").append(param2).append("\\\")\");\n");
			} else if (param1.equals("componentExoDoubleClick")) {
				sb.append("selenium.getEval(\"selenium.doComponentExoDoubleClick(\\\"").append(param2).append("\\\")\");\n");
			} else if (param1.equals("checkAndWait")) {
				sb.append("selenium.check(\"");
				sb.append(param2);
				sb.append("\");\n");
				sb.append("selenium.waitForPageToLoad(timeout);\n");
			} else if (param1.equals("storeAttribute")) {
				sb.append("String ").append(param3).append(" = ").append("selenium.getAttribute(\"").append(param2).append("\");\n");
			} else if (param1.equals("storeElementPositionTop")) {
				sb.append("String ").append(param3).append(" = ").append("selenium.getElementPositionTop(\"").append(param2).append("\");\n");
			} else if (param1.equals("verifyElementPositionTop")) {
				sb.append("verifyEquals(").append("\"").append(param3).append("\", ").append("selenium.getElementPositionTop(\"").append(param2).append("\"));\n");
			} else if (param1.equals("echo")) {
				sb.append("System.out.println(\"" + param2 + "\");\n");
			} else if (param1.length() > 0) {
				String message = param1 + " was not translated \"" + param2 + "\"";
				System.err.println("[ERROR] " + message);
				sb.append("// NOT GENERATED " + message);
				throw new RuntimeException("Selenium function not implemented : " + message);
			}
		}
		sb.append("}\n\n");
	}

	private String getTimeoutMessage(String param1) {
		return "if (second >= timeoutSecInt)\n fail(\"" + param1 +" reached a timeout (\" + timeoutSecInt + \"s)\");\n";
	}
	
	public static void writeFile(String file, String content) throws IOException {
		System.out.println("[INFO] Writing file : " + file);
		FileUtils.writeStringToFile(new File(file), content);
	}

	private String[] getParams(String step) throws Exception {
		String[] params = new String[3];

		int x = 0;
		int y = 0;

		for (int i = 0; i < 3; i++) {
			x = step.indexOf("<td>", x) + 4;
			y = step.indexOf("\n", x);
			y = step.lastIndexOf("</td>", y);
			params[i] = StringEscapeUtils.unescapeHtml(step.substring(x, y));
		}

		return params;
	}

	private String replace(String s, String oldSub, String newSub) {
		if ((s == null) || (oldSub == null) || (newSub == null)) {
			return null;
		}
		int y = s.indexOf(oldSub);
		if (y >= 0) {
			StringBuffer sb = new StringBuffer(s.length() + 5 * newSub.length());
			int length = oldSub.length();
			int x = 0;
			while (x <= y) {
				sb.append(s.substring(x, y));
				sb.append(newSub);
				x = y + length;
				y = s.indexOf(oldSub, x);
			}
			sb.append(s.substring(x));
			return sb.toString();
		} else {
			return s;
		}
	}

	private String fixParam(String param) {
		StringBuffer sb = new StringBuffer();

		char[] array = param.toCharArray();

		for (int i = 0; i < array.length; ++i) {
			char c = array[i];

			if (c == '\\') {
				sb.append("\\\\");
			} else if (c == '"') {
				sb.append("\\\"");
			} else if (Character.isWhitespace(c)) {
				sb.append(c);
			} /*
				 * else if ((c < 0x0020) || (c > 0x007e)) { sb.append("\\u");
				 * sb.append(UnicodeFormatter.charToHex(c)); }
				 */else {
				sb.append(c);
			}
		}
		return replace(sb.toString(), _FIX_PARAM_OLD_SUBS, _FIX_PARAM_NEW_SUBS);
	}

	private String replace(String s, String[] oldSubs, String[] newSubs) {
		if ((s == null) || (oldSubs == null) || (newSubs == null)) {
			return null;
		}

		if (oldSubs.length != newSubs.length) {
			return s;
		}

		for (int i = 0; i < oldSubs.length; i++) {
			s = replace(s, oldSubs[i], newSubs[i]);
		}

		return s;
	}

	private static final String replaceSeparatorPattern = File.separator.equals("\\") ? "\\\\" : File.separator;
	private static final String[] _FIX_PARAM_OLD_SUBS = new String[] { "\\\\n", "<br />" };
	private static final String[] _FIX_PARAM_NEW_SUBS = new String[] { "\\n", "\\n" };
	public static final String SLASH = "/";

}
