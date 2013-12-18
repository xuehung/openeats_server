package controllers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.*;
import com.smartfile.api.*;

import java.io.StringWriter;
import java.io.InputStream;
import java.io.File;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import play.db.jpa.*;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.*;

import securesocial.core.Identity;
import securesocial.core.java.SecureSocial;
import views.html.*;

import models.CSRecord;

public class CrdSrcController extends Controller {
    private static String sfKey = "Vb5EI1bHvLfCvJ3tRZ9z6GT559X6Ec";
    private static String sfPwd = "dveZC2HDtBB1rwk6FBl4JL944lSCJK";

    public static Result uploadForm() {
        return ok(upload.render()); 
    }

    @play.db.jpa.Transactional
    public static Result upload() {
	MultipartFormData body = request().body().asMultipartFormData();
	String playPath = Play.application().path().toString();

	// deal with the picture file
	FilePart[] pics = new FilePart[3];
	for(int i=0 ; i<pics.length ; i++) {
	    pics[i] = body.getFile("pic"+i);
	}

	//FilePart picture = body.getFile("pic1").getFilename();
	//String fileName = picture.getFilename();
	//String contentType = picture.getContentType();
	//File file = picture.getFile();

	// get the data
	Map<String, String[]> values = body.asFormUrlEncoded();
	String barcode = values.get("barcode")[0];

	// create the CSRecord
	CSRecord r = new CSRecord();
	r.barcode = barcode;
	r.picOneName = pics[0].getFilename();
	r.picTwoName = pics[1].getFilename();
	r.picThreeName = pics[2].getFilename();
	r.save();

	String result = "";

	try {
	    BasicClient api = new BasicClient(sfKey,sfPwd);
	    for(int i=0 ; i<pics.length ; i++) {
		File file = pics[i].getFile();
		// create the directory with record id
		api.post("/path/oper/mkdir/", "/", "path="+r.id);

		// upload the file to the specific directory that I just made
		api.post("/path/data"+"/"+r.id, "/" ,pics[i].getFile());

		// rename the file
		String srcPath = r.id + "/" + pics[i].getFile().getName();
		String dstPath = r.id + "/" + pics[i].getFilename();
		api.post("/path/oper/rename/", "/", "src="+srcPath+"&dst="+dstPath);
	    }
	} catch (Exception e) {
	    return ok(e.toString());
	}

	// create the directory
	File dir = new File(playPath+"/public/csimg/"+r.id);
	try {
	    FileUtils.forceMkdir(dir);
	} catch (Exception e){
	    return ok("Failed to create directory");
	}

	// Move the images
	try {
	    for(int i=0 ; i<pics.length ; i++) {
		File srcFile = pics[i].getFile();
		File dstFile = new File(dir.toString() + "/" + pics[i].getFilename()); // directory path + / + file name
		FileUtils.moveFile(srcFile, dstFile);
		//result += (dstFile.toString() + ":");
	    }
	} catch (Exception e) {
	    return ok("Failed to move files");
	}

	//return ok(Play.application().path()+":"+barcode+":"+fileName+":"+contentType);
	//return ok("success");
	return ok(result);
    }

    @play.db.jpa.Transactional
    public static Result inputForm() {
	// get the record
	Query query = JPA.em().createQuery("SELECT r FROM CSRecord r");
	List<CSRecord> rList = query.getResultList();
	CSRecord r = rList.get(rList.size()-1);
	
	String result = "";
	//get the url of the directory of images
	try {
	    BasicClient api = new BasicClient(sfKey,sfPwd);
	    InputStream is = api.post("/link/", "/", "path="+r.id+"&read=true&list=true");
	    StringWriter writer = new StringWriter();
	    IOUtils.copy(is, writer);
	    result = writer.toString();
	    JSONObject jsonObj = new JSONObject(result);
	    result = (String)jsonObj.get("href");
	} catch(Exception e) {
	}
	return ok(CSInputForm.render(r, result)); 
    }

}
