package com.Cube.Console;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.ActionDialect;

public class ConvertTask {

	StateCode state;
	String filePath = null;
	String filePrefix = null;
	String fileExtension = null;
	FileType fileType;
	String subPath = null;
	String taskTag = null;
	String tag = null;
	Cellet cellet = null;
	List<String> convertedFileList = null;

	private static final String rootPath = "/home/wwwroot/default/cubewhiteboard/shared/";
	private static final String URL = "http:211.103.217.154/cubewhiteboard/shared/";
	private final String UNOCONV_PDF = "unoconv -f pdf";
	private final String PDFTOPPM_PNG = "pdftoppm -png";
	private final String FILE_EXTENSION = "png";

	public ConvertTask(String filePath, String subPath, String tag,
			String taskTag) {
		this.filePath = filePath;
		this.filePrefix = ConvertUtils
				.extractFileNameWithoutExtension(filePath);
		this.fileExtension = FILE_EXTENSION;
		this.fileType = FileType.parseType(ConvertUtils
				.extractFileExtensionFromFilePath(filePath));
		this.subPath = subPath;
		this.tag = tag;
		this.taskTag = taskTag;
	}

	public static String getRootPath() {
		return rootPath;
	}

	public void setStateCode(StateCode state) {
		this.state = state;
	}

	public StateCode getStateCode() {
		return this.state;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		return this.tag;
	}

	public void setFilePath(String fileP) {
		this.filePath = fileP;
	}

	public String getFilePath() {
		return this.filePath;
	}

	public void setFilePrefix(String filePre) {
		this.filePrefix = filePre;
	}

	public String getFilePrefix() {
		return this.filePrefix;
	}

	public void setFileExtension(String fileExt) {
		this.fileExtension = fileExt;
	}

	public String getFileExtension() {
		return this.fileExtension;
	}

	public void setSubPath(String subPath) {
		this.subPath = subPath;
	}

	public String getSubPath() {
		return this.subPath;
	}

	public FileType getFileType() {
		return this.fileType;
	}

	public void setTaskTag(String taskTag) {
		this.taskTag = taskTag;
	}

	public String getTaskTag() {
		return this.taskTag;
	}

	public void setConvertedFileList(List<String> list) {
		this.convertedFileList = list;
	}

	public List<String> getConvertedFileList() {
		return this.convertedFileList;
	}

	// unoconv -f pdf /home/lztxhost/Documents/dddd.doc
	public void unoconvOperation(Cellet cellet) {

		this.cellet = cellet;

		this.state = StateCode.Executing;
		
		responseTaskState(this);
		
		String unoconvCmd = UNOCONV_PDF + " " + this.filePath;
//		System.out.println(this.getClass() + " : " + unoconvCmd);
		JavaExeLinuxCmd.execut(new String[] { "/bin/sh", "-c", unoconvCmd },
				null, null).toString();

	}
	
	// pdftoppm -png file.pdf file_prefix
	public void pdftoppmOperation(Cellet cellet) {

		this.cellet = cellet;

		this.state = StateCode.Executing;
		
		responseTaskState(this);
		
		int index = this.filePath.lastIndexOf(".");
		String pdfFilePath = this.filePath.substring(0, index + 1) + "pdf";

		if (null == this.filePrefix) {
			this.filePrefix = "Cube";
		}

		if (null == this.fileExtension) {
			this.fileExtension = "png";
		}

		String pdftoppmCmd = PDFTOPPM_PNG + " " + pdfFilePath + " "
				+ this.filePrefix;
//		System.out.println(this.getClass() + " : " + pdftoppmCmd);
		JavaExeLinuxCmd.execut(new String[] { "/bin/sh", "-c", pdftoppmCmd },
				null, null).toString();		
	}

	public List<String> moveFileToWorkspace(Cellet cellet) {
		this.cellet = cellet;
		this.state = StateCode.Executing;
		String error = null;
		responseTaskState(this);
		// 1 查找文件
		// find -name "*.png" | grep -H "MCE"
		String findCmd = "find -name '*." + this.getFileExtension()
				+ "' | grep -H " + "'" + this.getFilePrefix() + "'";
//		System.out.println(this.getClass() + " : " + findCmd);
		String fileNames = JavaExeLinuxCmd.execut(
				new String[] { "/bin/sh", "-c", findCmd }, null, null)
				.toString();

		// 2 提取转换后 文件名 数组
		List<String> fileNameArray = new ArrayList<String>();
		if (fileNames.length() > 0) {
			String[] names = fileNames.split("\n");
			for (String fileName : names) {
				int index = fileName.lastIndexOf("/");
				String subStr = fileName
						.substring(index + 1, fileName.length());
				fileNameArray.add(subStr);
			}
		}
		else {
			error = "404";
		}

		// 3 移动转换后的文件到工作目录

		int endIndex = filePath.lastIndexOf("/");
		String uploadDirPath = filePath.substring(0, endIndex + 1);
		// uploadDirPath： /home/lztxhost/Documents/upload
		String workDirPath = rootPath + subPath;
		// workDirPath： /home/wwwroot/default/cubewhiteboard/shared/ + subPath
		File tmpFile = new File(workDirPath);
		if (!tmpFile.exists()) {
			tmpFile.mkdirs();
		}

		List<String> convertedFileURLArray = new ArrayList();
		if (fileNameArray.size() > 0) {
			for (String name : fileNameArray) {
				String pngFilePath = uploadDirPath + name;
				convertedFileURLArray.add(URL + subPath + name);

				// mv -t /opt/soft/test/test4/ log1.txt log2.txt log3.txt
				String moveCmd = "mv -t " + workDirPath + " " + pngFilePath;
//				 System.out.println(this.getClass() + " MOVE_PNG_FILE : " +
//				 moveCmd);
				String r = JavaExeLinuxCmd.execut(
						new String[] { "/bin/sh", "-c", moveCmd }, null, null)
						.toString();
			}
		}
		else 
		{
			convertedFileURLArray.add(error);
		}
		// 返回URLs
		return convertedFileURLArray;
	}

	private void responseTaskState(ConvertTask task) {
		int state = task.state.getCode();
		ActionDialect ad = new ActionDialect();
		ad.setAction(CubeConsoleAPI.ACTION_CONVERT_STATE);
		JSONObject jo = new JSONObject();
		try {
			jo.put("state", state);
			jo.put("filePath", task.getFilePath());
			jo.put("filePrefix", task.getFilePrefix());
			jo.put("fileExtension", task.getFileExtension());
			jo.put("subPath", task.getSubPath());
			jo.put("taskTag", task.getTaskTag());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		ad.appendParam("data", jo);

		// 发送数据
		cellet.talk(this.getTag(), ad);
	}
}
