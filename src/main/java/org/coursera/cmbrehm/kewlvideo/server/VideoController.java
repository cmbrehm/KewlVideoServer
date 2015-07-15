/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.coursera.cmbrehm.kewlvideo.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.coursera.cmbrehm.kewlvideo.server.model.Video;
import org.coursera.cmbrehm.kewlvideo.server.model.VideoStatus;
import org.coursera.cmbrehm.kewlvideo.server.model.VideoStatus.VideoState;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

	/**
	 * You will need to create one or more Spring controllers to fulfill the
	 * requirements of the assignment. If you use this file, please rename it
	 * to something other than "AnEmptyController"
	 * 
	 * 
		 ________  ________  ________  ________          ___       ___  ___  ________  ___  __       
		|\   ____\|\   __  \|\   __  \|\   ___ \        |\  \     |\  \|\  \|\   ____\|\  \|\  \     
		\ \  \___|\ \  \|\  \ \  \|\  \ \  \_|\ \       \ \  \    \ \  \\\  \ \  \___|\ \  \/  /|_   
		 \ \  \  __\ \  \\\  \ \  \\\  \ \  \ \\ \       \ \  \    \ \  \\\  \ \  \    \ \   ___  \  
		  \ \  \|\  \ \  \\\  \ \  \\\  \ \  \_\\ \       \ \  \____\ \  \\\  \ \  \____\ \  \\ \  \ 
		   \ \_______\ \_______\ \_______\ \_______\       \ \_______\ \_______\ \_______\ \__\\ \__\
		    \|_______|\|_______|\|_______|\|_______|        \|_______|\|_______|\|_______|\|__| \|__|
                                                                                                                                                                                                                                                                        
	 * 
	 */
	
	private AtomicLong idGenerator;
	private HashMap<Long,Video> videoList;
	
	public VideoController() {
		idGenerator = new AtomicLong(1l);
		videoList = new HashMap<Long,Video>();
	}
	
	@RequestMapping(value="/video", method = RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideos() {
		return videoList.values();
	}
	
	@RequestMapping(value="/video", method = RequestMethod.POST)
	public @ResponseBody Video saveVideo(@RequestBody Video videoIn) {
		long id = idGenerator.incrementAndGet();
		videoIn.setId(id);
		videoIn.setDataUrl(getDataUrl(id));
		videoList.put(id,videoIn);
		return videoIn;
	}
	
	@RequestMapping(value="/video/{id}/data", method = RequestMethod.GET)
	public void retrieveVideoData(@PathVariable("id") long id, HttpServletResponse response) {
		try {
			VideoFileManager vfmgr = VideoFileManager.get();
			Video video = videoList.get(id);
			if (video!=null && vfmgr.hasVideoData(video)) {
				OutputStream outStream = response.getOutputStream();
				response.setContentType(video.getContentType());
				vfmgr.copyVideoData(video,outStream);
			} else {
				response.setStatus(HttpStatus.NOT_FOUND.value());
			}
		} catch (IOException iox) {
			try {
				response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), iox.getMessage());
			} catch (Throwable t) { }
		}
			
		
	}

	@RequestMapping(value="/video/{id}/data", method = RequestMethod.POST)
	public ResponseEntity<VideoStatus> saveVideoData(@PathVariable("id") long id, 
			@RequestPart("data") MultipartFile videoData) {
		ResponseEntity<VideoStatus> response;
		try {
			InputStream videoDataStream = videoData.getInputStream();
			VideoFileManager vfmgr = VideoFileManager.get();
			Video video = videoList.get(id);
			if (video != null) {
				vfmgr.saveVideoData(video, videoDataStream);
				response =  new ResponseEntity<VideoStatus>(new VideoStatus(VideoState.READY), HttpStatus.ACCEPTED);
			} else {
				response = new ResponseEntity<VideoStatus>(HttpStatus.NOT_FOUND);
			}
			} catch (IOException iox) {
			response = new ResponseEntity<VideoStatus>(HttpStatus.INTERNAL_SERVER_ERROR);
		} 
		return response;
	}
	
	private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
       HttpServletRequest request = 
           ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
       String base = 
          "http://"+request.getServerName() 
          + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
       return base;
    }

	
}
