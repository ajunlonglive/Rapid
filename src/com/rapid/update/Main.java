package com.rapid.update;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;

import com.rapid.utils.Files;
import com.rapid.utils.ZipFile;

public class Main {

	// Maintain the updater here, but move to separate RapidUpdate project to export the runnable .jar so it's 20k (rather than 20meg when exported from the Rapid)

	private static final String VERSION = "1.8";

	private static UpdateOutput _output;

	private static boolean _replaceOnly = false;

	public static void main(String[] args) {

		// check args for replace and set if so (we'll tell the user later)
		if (args != null && args.length > 0 && args[0].equalsIgnoreCase("replace")) _replaceOnly = true;

		// check if we have graphics support
		if (GraphicsEnvironment.isHeadless()) {

			// create the console output
			ConsoleOutput consoleOutput = new ConsoleOutput();

			// assign to the output object
			_output = consoleOutput;

		} else {

			// Create and set up the window.
	        JFrame frame = new JFrame("Rapid Update - " + VERSION);
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	        // create the gui output
	        GUIOutput guiOutput = new GUIOutput();

	        // Add content to the window.
	        frame.add(guiOutput);

	        // Display the window.
	        frame.pack();
	        frame.setVisible(true);

	        // assign to output object
	        _output = guiOutput;

		}

		try {

			// say we've started
			_output.log("Rapid Update - " + VERSION + " started");

			// if replace only
			if (_replaceOnly) _output.log("Only replacing files already in the application");

			_output.log("Identifying update location");

			ClassLoader classLoader  = ClassLoader.getSystemClassLoader();

			if (classLoader == null) _output.log("Class loader is null");

			URL resource = classLoader.getResource(".");

			if (resource == null) _output.log("Resource is null");

			// get the current path
			String path = resource.getPath();

			_output.log("Path is " + path);

			// url decode it (for spaces on windoze)
			path = URLDecoder.decode(path,"UTF-8");

			// location is the WEB-INF folder
			File location = new File (path).getParentFile();

			// if we're running under Eclipse, during development
			if ("git".equals(location.getParentFile().getName())) {
				// change accordingly
				location = new File(location.getAbsolutePath() + "/WebContent/WEB-INF/");
			}

			// the root will be the parent of the WEB-INF folder
			File root = location.getParentFile();

			_output.log("Update location found");

			File updateFolder = null;
			for (File folder : location.listFiles()) {
				if (folder.getName().equals("update") && folder.isDirectory()) {
					updateFolder = folder;
					break;
				}
			}
			if (updateFolder == null) throw new Exception("'update' folder not found");

			// a simple date formatter
			SimpleDateFormat df = new SimpleDateFormat("yyMMdd-HHmmss");

			// add current date and time to update folder
			File workingFolder = new File(updateFolder + "/" + df.format(new Date()));

			/////////////////////////////////// Archive //////////////////////////////////

			/*

			// derive the archive folder
			File archiveFolder = new File(workingFolder.getAbsolutePath() + "/archive");

			_output.log("Archiving " + root + " to " + archiveFolder);

			// make it's dirs
			archiveFolder.mkdirs();

			// Start creating the archive!

			// do not include these files/folders in the archive
			List<String> ignoreFiles = new ArrayList<>();
			ignoreFiles.add(".gitignore");
			ignoreFiles.add("applications");
			ignoreFiles.add("uploads");
			ignoreFiles.add("temp");
			ignoreFiles.add("WEB-INF");

			// copy root to archiveFolder, but ignore: uploads, temp, and WEB-INF
			Files.copyFolder(root, archiveFolder, ignoreFiles);

			_output.log("Archived WebContent");

			// remove WEB-INF from ignore as we're going to copy it with it's own further ignores
			ignoreFiles.remove("WEB-INF");
			// add _backups, logs, and update to ignore inside WEB-INF
			ignoreFiles.add("_backups");
			ignoreFiles.add("logs");
			ignoreFiles.add("update");

			// update archive folder to WEB-INF
			archiveFolder = new File(archiveFolder.getAbsolutePath() + "/WEB-INF");

			// make it's dir
			archiveFolder.mkdir();

			// copy WEB-INF
			Files.copyFolder(new File(root.getAbsolutePath() + "/WEB-INF"), archiveFolder, ignoreFiles);

			_output.log("Archived WEB-INF");

			// update archive folder to Rapid application back-end
			archiveFolder = new File(archiveFolder.getAbsolutePath() + "/applications/rapid");

			// make it's dirs
			archiveFolder.mkdirs();

			// copy rapid application back-end
			Files.copyFolder(new File(root.getAbsolutePath() + "/WEB-INF/applications/rapid"), archiveFolder);

			// update archive folder to Rapid application front-end
			archiveFolder = new File(workingFolder.getAbsolutePath() + "/archive/applications/rapid");

			// make it's dirs
			archiveFolder.mkdirs();

			// copy rapid application front-end
			Files.copyFolder(new File(root.getAbsolutePath() + "/applications/rapid"), archiveFolder);

			_output.log("Archived Rapid application");

			*/

			/////////////////////////////////// Update //////////////////////////////////

			_output.log("Locating update(s)");

			// look for any .war or .zip files in update folder
			List<File> unzipFiles = new ArrayList<>();
			for (File file : updateFolder.listFiles()) {
				if (file.getName().endsWith(".war") || file.getName().endsWith(".zip")) {
					unzipFiles.add(file);
				}
			}

			// check for update files
			if (unzipFiles.size() == 0) {

				_output.log("No update .war or .zip files found");

			} else {

				// derive the unzip folder
				File unzipFolder = new File(workingFolder.getAbsolutePath() + "/update");

				_output.log("Found " + unzipFiles.size() + " file" + (unzipFiles.size() == 1 ? "" : "s") + " " + unzipFiles + ", unzipping to " + unzipFolder);

				// loop the update files
				for (File unzipFile : unzipFiles) {

					// make a zip file object
					ZipFile zipFile = new ZipFile(unzipFile);
					// unzip it (this will make any directories we need)
					zipFile.unZip(unzipFolder);

				}

				// now loop the folders looking for ones we recognise and updating as per rules
				for (File file : unzipFolder.listFiles()) {

					// get the file name
					String fileName = file.getName();

					// identify special files/folders to ignore
					if (file.isFile() && (fileName.equals("manifest.json") || fileName.startsWith("__MAC"))) {

						// log that we're ignoring
						_output.log("Ignoring " + fileName + " " + (file.isDirectory() ? "folder" : "file"));

					} else if (file.isDirectory() && file.getName().equals("applications")) {

						// look for the rapid folder
						File rapidFolder = new File(file + "/rapid");
						// if we've found the rapid folder
						if (rapidFolder.exists() && rapidFolder.isDirectory()) {
							// log
							_output.log("Updating Rapid application web resources");
							// copy
							Files.copyFolder(rapidFolder, new File(root + "/applications/rapid"));
						}

					} else if (file.isDirectory() && file.getName().equals("WEB-INF")) {

						// list the subfolders of the WEB-INF folder
						for (File folder : file.listFiles()) {

							// we're only interested in visible folders - this spares the web.xml and update.jar
							if (folder.isDirectory() && !folder.isHidden()) {

								// if we've found the applications folder
								if (folder.getName().equals("applications")) {

									// look for the rapid folder - NOTE we are assuming update will always be version 1!!!!
									File rapidFolder = new File(folder + "/rapid/1");
									// if we've found the rapid folder
									if (rapidFolder.exists() && rapidFolder.isDirectory()) {

										// get the application.xml file
										File appXmlFile = new File(rapidFolder + "/application.xml");
										// get the update xml file
										File appXmlFileUpdate =  new File(root + "/WEB-INF/applications/rapid/1/application.xml");
										// if it exists, copy it
										if (appXmlFile.exists() && appXmlFileUpdate.exists()) {
											// log
											_output.log("Updating Rapid application xml file");
											// copy it always
											Files.copyFile(appXmlFile, appXmlFileUpdate);
										}

										// get the pages folder
										File pagesFolder = new File(rapidFolder + "/pages");
										// get the update pages folder
										File pagesFolderUpdate = new File(root + "/WEB-INF/applications/rapid/1/pages");
										// if it exists, copy it
										if (pagesFolder.exists() && pagesFolderUpdate.exists()) {
											// log
											_output.log("Updating Rapid application pages");
											// copy it always
											Files.copyFolder(pagesFolder, pagesFolderUpdate);
										}
									}

								} else if (folder.getName().equals("classes")) {

									// this is the classes folder - ignore any files in it's root as they are likely to be log4j config files

									// list the sub-folders of the classes folder
									for (File classFolder : folder.listFiles()) {

										// we're only interested in visible folders - this spares the files that may be in here like the log4j config file
										if (classFolder.isDirectory() && !classFolder.isHidden()) {

											// log that we're merging
											_output.log("Merging " + classFolder.getName() + " folder");

											// merge the classes sub-folders with the replace only flag
											Files.copyFolder(classFolder, new File(root + "/WEB-INF/classes/" + classFolder.getName()), _replaceOnly);

										}

									}

								} else if (folder.getName().equals("update")) {

									// get just the update.jar file
									File updateJarFile = new File(folder + "/update.jar");
									// if we got one
									if (updateJarFile.exists()) {

										// log that we're copying
										_output.log("Copying update.jar file");

										// new update file destination
										File newUpdateJarFile = new File(root + "/WEB-INF/update/update.jar");

										// make any folders we need (highly unlikely but good for testing)
										newUpdateJarFile.getParentFile().mkdirs();

										// copy it
										Files.copyFile(updateJarFile, newUpdateJarFile);

									}

								} else if (folder.getName().equals("custombuild") ||
										folder.getName().equals("database") ||
										folder.getName().equals("devices") ||
										folder.getName().equals("logs") ||
										folder.getName().equals("processes") ||
										folder.getName().equals("temp")) {

									// log that we're ignoring
									_output.log("Ignoring " + folder.getName() + " folder");

								} else {

									// log that we're merging
									_output.log("Merging " + folder.getName() + " folder");

									// merge all other folders
									Files.copyFolder(folder, new File(root + "/WEB-INF/" + folder.getName()));

								}

							} else {

								// log that we're ignoring
								_output.log("Ignoring " + folder.getName() + " " + (folder.isDirectory() ? "folder" : "file"));

							} // directory / not hidden check

						}

					} else {

						// merge/copy all unrecognised folders/files

						if (file.isDirectory()) {

							_output.log("Merging " + file.getName() + " folder");

							Files.copyFolder(file, new File(root + "/" + file.getName()), _replaceOnly);

						} else {

							_output.log("Copying file " + file.getName());

							Files.copyFile(file, new File(root + "/" + file.getName()), _replaceOnly);

						}

					} // folder / file check

					/////////////////////////////////// Delete //////////////////////////////////

					// look in the update for a delete.txt file
					File deleteFile = new File(unzipFolder.getAbsolutePath() + "/WEB-INF/update/delete.txt");

					// if we found one
					if (deleteFile.exists()) {

						// log
						_output.log("Delete file found!");

						// make a scanner for this file
						Scanner sc = new Scanner(deleteFile);

						// set to new lines (it seems to break on spaces by default)
						sc.useDelimiter("\n");

						// if there is a next (nextLine resulted in a null exception)
				        while (sc.hasNext()) {

				        	// get this line
				        	String line = sc.next();

				        	// if there was a line
				        	if (line != null) {

				        		// trim it for good measure
				        		line = line.trim();

				        		// if it's long enough (empty will delete the root!), isn't a comment, and doesn't contain dangerous characters
					        	if (line.length() > 4 && !line.startsWith("#") && !line.contains("..")) {

					        		// file object for what we're about to delete
					        		File fileToDelete = new File(root.getAbsolutePath() + line);

					        		// if it exists
					        		if (fileToDelete.exists()) {

					        			_output.log("Deleting " + line);

					        			fileToDelete.delete();

					        		}

					        	}

				        	}

				        }
				        sc.close();

					}

				} // update folder files loop

				// loop the update files again
				for (File unzipFile : unzipFiles) {

					// copy them into the working folder
					Files.copyFile(unzipFile, workingFolder);

					// delete them from the update folder to clean up the root space
					unzipFile.delete();

					// log
					_output.log("Moved update file " + unzipFile.getName() + " to " + workingFolder);

				}

			} // .jar or .zip files

			/////////////////////////////////// Clean up //////////////////////////////////

			// finally some cleanup of the Rapid instance

			// get the logs folder
			File logsFolder = new File(root + "/WEB-INF/logs");
			// check it exists
			if (logsFolder.exists()) {
				_output.log("Clearing logs folder");
				// loop the files
				for (File file : logsFolder.listFiles()) {
					// delete
					Files.deleteRecurring(file);
				}
			}

			// get the temp folder
			File tempFolder = new File(root + "/WEB-INF/temp");
			// check it exists
			if (tempFolder.exists()) {
				_output.log("Clearing temp folder");
				// loop the files
				for (File file : tempFolder.listFiles()) {
					// delete
					Files.deleteRecurring(file);
				}
			}

			_output.log("Done!");

		} catch (Exception ex) {

			_output.log("Error : " + ex.getMessage());

		}

	}

}
