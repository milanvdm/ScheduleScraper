package scraper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import schedule.CourseMoment;
import util.Browser;
import util.BrowserImpl;
import util.Parser;

public class CourseScraperImpl implements CourseScraper {
	
	public Browser browser = new BrowserImpl();
	
	private final static String COURSE_SEARCH_RESULTS = "div#results";
	private final static String COURSE_SEARCH_HIT_CLASS = "search-result";
	private final static String COURSE_SEARCH_HIT_TITLE = "title";
	
	private final static String COURSE_CONTENT = "div#content";
	private final static String COURSE_DURATION_CLASS = "span.duur";
	

	public List<CourseMoment> getCourseMoments(String courseUrl, Date weekDate) throws URISyntaxException, IOException, ParseException, InterruptedException {
		
		String weekHtml = getCourseHtmlAtWeek(courseUrl, weekDate);
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(weekDate);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		Date startDate = cal.getTime();
		cal.add(Calendar.DAY_OF_WEEK, 6);
		Date endDate = cal.getTime();  
		
		Calendar start = Calendar.getInstance();
		start.setTime(startDate);
		Calendar end = Calendar.getInstance();
		end.setTime(endDate);

		List<CourseMoment> courseMoments = new ArrayList<CourseMoment>();
		
		for (Date day = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), day = start.getTime()) {
			courseMoments.addAll(getCourseMomentsForDay(weekHtml, day));
		}
		
		return courseMoments;
		
	}
	
	private List<CourseMoment> getCourseMomentsForDay(String courseHtml, Date day) throws ParseException {
		List<CourseMoment> courseMoments = new ArrayList<CourseMoment>();
		
		Document document = Jsoup.parse(courseHtml);
		
		Elements dayElements = document.getElementsByAttributeValue("align", "middle");
		
		for(Element dayElement: dayElements) {
			
			Element toCheck = dayElement.getElementsByClass("menu").first();
			
			if(toCheck == null) {
				continue;
			}
			
			String possibleDate = toCheck.text();
			Date date = Parser.parseDate(possibleDate);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			if(sdf.format(date).equals(sdf.format(day))) {
				Elements hourElements = dayElement.getElementsByClass("event");
				
				for(Element hourElement: hourElements) {
					
					String info = hourElement.attr("onmouseover");
					CourseMoment courseMoment = Parser.parseCourseMoment(info);
					courseMoment.setDate(day);
					
					courseMoments.add(courseMoment);
				}
				
				return courseMoments;
			}
		}
		
		return courseMoments;
	}
	
	
	private String getCourseHtmlAtWeek(String courseUrl, Date weekDate) throws IOException, InterruptedException {
		String courseScheduleUrl = getCourseScheduleUrl(courseUrl);
		
		browser.waitForRedirection(courseScheduleUrl);
		
		return browser.getPageSource();
	}
	
	private String getCourseScheduleUrl(String courseUrl) throws IOException {
		
		Document document = Jsoup.connect(courseUrl).get();
		
		Element courseContent = document.select(COURSE_CONTENT).first();
		
		Element courseDuration = courseContent.select(COURSE_DURATION_CLASS).first();
		
		String scheduleUrlWithJs = courseDuration.getElementsByAttribute("href").attr("href");
		
		String scheduleUrl = Parser.getScheduleUrl(scheduleUrlWithJs);
		
		return scheduleUrl;
		
	}
	
	public String getCourseUrl(String query) throws URISyntaxException {
		String queryUrl = courseQueryUrl(query);
		
		browser.waitForJS(queryUrl);
		
		String pageSource = browser.getPageSource();
		
		Document document = Jsoup.parse(pageSource);
		
		Element searchResults = document.select(COURSE_SEARCH_RESULTS).first();
		Element result = searchResults.getElementsByClass(COURSE_SEARCH_HIT_CLASS).first();
		
		String url = result.getElementsByClass(COURSE_SEARCH_HIT_TITLE).first().getElementsByAttribute("href").attr("href");
	
		return url;
	}
	
	private String courseQueryUrl(String query) throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder("https://onderwijsaanbod.kuleuven.be");
		
		URIBuilder queryUri = new URIBuilder();
		queryUri.addParameter("q", query)
				.addParameter("idx", "ALL")
				.addParameter("jaar", "2015")
				.addParameter("isvertaling", "0");
		
		
		uriBuilder	.setPath("/oa/find/")
					.setFragment("/" + queryUri.toString());
		
		
		return uriBuilder.toString().replace("+", "%20");
	}

}
