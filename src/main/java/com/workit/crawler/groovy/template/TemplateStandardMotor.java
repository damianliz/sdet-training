package com.workit.crawler.groovy.template;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.workit.b2b.common.*;
import com.workit.b2b.server.property.PropertyService;
import com.workit.b2b.server.service.manager.IUniverseManager;
import com.workit.b2b.spring.SpringApplicationContext;
import com.workit.crawl.exception.InvalidConnectionException;
import com.workit.crawl.exception.InvalidPageException;
import com.workit.crawl.logger.IEngineLogger;
import com.workit.crawl.util.UrlConsolidator;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.checkdigit.EAN13CheckDigit;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class TemplateStandardMotor extends SiteEnginePass {

	private static final String SITE_NAME = "Site Name"; // TODO

	private static final String PROXY_CONFIGURATION = "motors.standard.proxy";
	private static final String PROXY_PORT_CONFIGURATION = "motors.standard.proxy.port";
	private static final String MOTOR_SITE_COUNTRY = "motor.site.%s.country";

	private static final String DEFAULT_STANDARD_PROXY = "proxy-out.workit.fr";
	private static final String DEFAULT_STANDARD_PROXY_PORT = "3129";

	private static final int MAX_PAGE_LIMIT = 500;
	private static final int MAX_PAGE_RETRY_COUNT = 5;
	private static final int MIN_DUPLICATED_PRODUCTS = 3;

	private static final Locale CURRENT_LOCALE = Locale.getDefault(); // TODO ex : Locale.FRENCH

	private static final PeriodFormatter DELIVERY_PERIOD_PARSER = PeriodFormat.wordBased(CURRENT_LOCALE); // TODO
	private static final DateTimeFormatter DELIVERY_DATE_PARSER = DateTimeFormat.forPattern("dd-MM-yyyy")
			.withLocale(CURRENT_LOCALE); // TODO
	private static final DateTimeFormatter REVIEW_DATE_PARSER = DateTimeFormat.forPattern("dd/MM/yyyy")
			.withLocale(CURRENT_LOCALE); // TODO
	private static final LocalDate NOW = LocalDate.now();

	private static final Pattern DELIVERY_PERIOD_PATTERN = Pattern.compile("%delivery period pattern%"); // TODO
	private static final Pattern DELIVERY_DATE_PATTERN = Pattern.compile("%delivery date pattern%"); // TODO
	private static final Pattern REVIEW_DATE_PATTERN = Pattern.compile("%review date pattern%"); // TODO

	private String proxy;
	private int proxyPort;

	private String host;
	private String country;

	private IUniverseManager universeManager;

	private DefaultHttpClient httpClient;

	private UrlConsolidator urlConsolidator;

	private int duplicateCount = 0;
	private List<String> lastProductPaths = Lists.newArrayList();
	private List<String> lastTitles = Lists.newArrayList();

	private HashSet<String> skuSet = Sets.newHashSet();

	private String getPathSegment() {
		String path = StringUtils.EMPTY;
		Universe universe = universeManager.getUniverse(segment.getId());
		while (universe != null) {
			String universeName = universe.getTitle();
			if (StringUtils.isBlank(path)) {
				path = universeName;
			} else {
				path = universeName + "\\" + path;
			}
			universe = universeManager.getUniverse(universe.getPid());
		}
		if (path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private void logSegmentProperties() {
		conn.debug("...........................");
		conn.debug("Segment Id .... : " + segment.getId());
		conn.debug("Segment Rpid .. : " + segment.getRpid());
		conn.debug("Segment Path .. : " + getPathSegment());
		conn.debug("Segment Url ... : " + segment.getUrl());
		conn.debug("Country ....... : " + country);
		conn.debug("Proxy ......... : " + proxy);
		conn.debug("...........................");
	}

	private void configureEngine() {
		configureSite();

		universeManager = SpringApplicationContext.getBean(IUniverseManager.class);
		PropertyService propertyService = SpringApplicationContext.getBean(PropertyService.class);

		proxy = propertyService.getPropertieValue(PROXY_CONFIGURATION, DEFAULT_STANDARD_PROXY);
		proxyPort = Integer
				.valueOf(propertyService.getPropertieValue(PROXY_PORT_CONFIGURATION, DEFAULT_STANDARD_PROXY_PORT));

		httpClient = createClient();

		country = propertyService.getPropertieValue(String.format(MOTOR_SITE_COUNTRY, segment.getSid()));

		logSegmentProperties();
	}

	private void configureSite() {
		try {
			final URL url = new URL(segment.getUrl());
			host = url.getHost();
			final String protocol = url.getProtocol();
			urlConsolidator = new UrlConsolidator(protocol + "://" + host + "/");
		} catch (MalformedURLException exc) {
			conn.error("Malformed URL [" + segment.getUrl() + "]" + withException(exc));
			conn.debug(ExceptionUtils.getStackTrace(exc));
		}
	}

	private DefaultHttpClient createClient() {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpHost proxyHost = new HttpHost(proxy, proxyPort);

		HttpRoutePlanner routePlanner = new HttpRoutePlanner() {
			@Override
			public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context)
					throws HttpException {
				return new HttpRoute(target, null, new HttpHost(proxy, proxyPort),
						"https".equalsIgnoreCase(target.getSchemeName()));
			}
		};

		client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxyHost);
		client.setRoutePlanner(routePlanner);
		return client;
	}

	private PageRequest get(final String url) {
		return new PageRequest().withMethod(HttpMethod.GET).withUrl(url);
	}

	private PageRequest post(final String url, final String entity) {
		return new PageRequest().withMethod(HttpMethod.POST).withEntity(entity).withUrl(url);
	}

	private void paginateThroughSegments() {
		PageRequest request = get(segment.getUrl());
		try {
			int loop = 0;
			boolean continueCrawl = true;
			while (continueCrawl) {
				loop++;
				PageResponse listingPage = connection(request, "listing page");
				checkNotNull(listingPage, "No listing page returned");
				request.withCookie(listingPage.getSetCookie());

				Document listingPageDocument = Jsoup.parse(listingPage.getContent(), request.getUrl());
				handleListingPage(listingPageDocument, request.getUrl(), loop);

				request = getNextRequest(listingPageDocument, request);
				conn.debug("Page " + loop + ". Next page URL : " + request.getUrl());
				continueCrawl = hasMorePages(request, loop);
			}
			Thread.sleep(2500);
		} catch (RuntimeException exc) {
			conn.warn("Page error [" + request.getUrl() + "]" + withException(exc));
		} catch (Exception exc) {
			conn.error("Main error [" + request.getUrl() + "]" + withException(exc));
			conn.debug(ExceptionUtils.getStackTrace(exc));
		}
		logStatistics();
	}

	private void logStatistics() {
		showResult("Offers crawled and saved : " + conn.getProducts().size());
		conn.debug(SITE_NAME + ", waiting few seconds before next segment!");
	}

	private Elements getOffers(final Document listingPageDocument) {
		Elements offersElements = listingPageDocument.select("Offers selector ..."); // TODO
		conn.debug("Offers on this page : " + offersElements.size());
		return offersElements;
	}

	private void saveIfNewProduct(SiteProduct siteProduct) {
		if (!isAlreadyCrawled(siteProduct.getSku())) {
			saveSiteProduct(siteProduct);
		}
	}

	private void handleListingPage(final Document listingPageDocument, final String url, final int loop)
			throws Exception {
		Elements offersElements = getOffers(listingPageDocument);
		int validOfferIndex = 0;
		int offerIndex = 0;
		for (Element offerElement : offersElements) {
			offerIndex++;
			showOffer("Page :: " + loop + " - Crawl Offer :: " + offerIndex);

			SiteProduct siteProduct = extractOfferInformation(offerElement, url);
			if (siteProduct != null) {
				if (isDuplicatedPage(siteProduct.getTitle(), siteProduct.getProductPath(), validOfferIndex)) {
					conn.error("Duplicate page found ==> Crawl stopped");
					throw new Exception("Endless loop stopped" + withSegmentId());
				}
				saveIfNewProduct(siteProduct);
				validOfferIndex++;
			}
		}
	}

	private boolean isDuplicatedPage(final String title, final String productPath, final int offerIndex) {
		if (offerIndex < MIN_DUPLICATED_PRODUCTS) {
			if (lastProductPaths.size() < offerIndex + 1 && lastTitles.size() < offerIndex + 1) {
				duplicateCount = 0;
				lastProductPaths.add(productPath);
				lastTitles.add(title);
			} else {
				if (lastProductPaths.get(offerIndex).equals(productPath) && lastTitles.get(offerIndex).equals(title)) {
					duplicateCount++;
				} else {
					duplicateCount = 0;
					lastProductPaths.set(offerIndex, productPath);
					lastTitles.set(offerIndex, title);
				}
			}
			return duplicateCount == MIN_DUPLICATED_PRODUCTS;
		}
		duplicateCount = 0;
		return false;
	}

	private boolean isAlreadyCrawled(final String sku) {
		if (skuSet.contains(sku)) {
			conn.debug("SKU already crawled [" + sku + "] ==> Skipping ...");
			return true;
		}
		skuSet.add(sku);
		return false;
	}

	private void saveSiteProduct(SiteProduct siteProduct) {
		conn.addProduct(siteProduct);
		conn.debug("<i style='color: green;'>Product added</i>");
	}

	private SiteProduct extractOfferInformation(final Element offerElement, final String url) {
		try {
			SiteProduct siteProduct = new SiteProduct();

			String title = getTitle(offerElement, url);
			conn.debug("Title : " + title);
			siteProduct.setTitle(title);

			String productPath = getProductPath(offerElement, url);
			conn.debug("Product path : " + productPath);
			siteProduct.setProductPath(productPath);

			return handleProductPage(siteProduct);
		} catch (NullPointerException exc) {
			conn.error("Offer error [" + url + "]" + withException(exc));
			return null;
		}
	}

	private void addBrandToTitle(SiteProduct siteProduct) {
		if (StringUtils.isNotBlank(siteProduct.getBrand())
				&& !StringUtils.containsIgnoreCase(siteProduct.getTitle(), siteProduct.getBrand())) {
			String title = Joiner.on(" ").skipNulls().join(Strings.emptyToNull(siteProduct.getBrand()),
					siteProduct.getTitle());
			conn.debug("Title with brand : " + title);
			siteProduct.setTitle(title);
		}
	}

	private SiteProduct handleProductPage(final SiteProduct siteProduct) {
		String url = siteProduct.getProductPath();
		PageResponse productPage = connection(get(url), "product page");
		// Response productPage = connection(post(url, "%Form data%"), "product
		// page");// TODO
		checkNotNull(productPage, "No product page returned");

		Document productPageDocument = Jsoup.parse(productPage.getContent(), url);
		String imagePath = getImagePath(productPageDocument, url);
		conn.debug("Image path : " + imagePath);
		siteProduct.setImagePath(imagePath);

		float price = getPrice(productPageDocument, url);
		conn.debug("Price : " + price);
		siteProduct.setPrice(price);

		Float previousPriceFromSite = getPreviousPriceFromSite(productPageDocument, url);
		conn.debug("Previous price from site : " + previousPriceFromSite);
		siteProduct.setPreviousPriceFromSite(previousPriceFromSite);

		Float recommendedRetailPrice = getRecommendedRetailPrice(productPageDocument, url);
		conn.debug("Recommended retail price : " + recommendedRetailPrice);
		siteProduct.setRecommendedRetailPrice(recommendedRetailPrice);

		Float cashBackPrice = getCashBackPrice(productPageDocument, url);
		conn.debug("Cashback price : " + cashBackPrice);
		siteProduct.setCashBackPrice(cashBackPrice);

		String specialPriceText = getSpecialPriceText(productPageDocument, url);
		conn.debug("Special price text : " + specialPriceText);
		siteProduct.setSpecialPriceText(specialPriceText);

		boolean isSpecialPricePresent = StringUtils.isNotBlank(specialPriceText);
		conn.debug("Special price present : " + isSpecialPricePresent);
		siteProduct.setSpecialPricePresent(isSpecialPricePresent);

		String availabilityText = getAvailabilityText(productPageDocument, url);
		conn.debug("Availability text : " + availabilityText);

		boolean availability = getAvailability(availabilityText, url);
		conn.debug("Availability : " + availability);
		siteProduct.setAvailable(availability);

		String brand = getBrand(productPageDocument, url);
		conn.debug("Brand : " + brand);
		siteProduct.setBrand(brand);

		addBrandToTitle(siteProduct);

		String sku = getSKU(productPageDocument, url);
		conn.debug("SKU : " + sku);
		siteProduct.setSku(sku);

		String mpn = getMPN(productPageDocument, url);
		conn.debug("MPN : " + mpn);
		siteProduct.setMpn(mpn);

		String ean = getEAN(productPageDocument, url);
		conn.debug("EAN : " + ean);
		siteProduct.setEAN(ean);

		String rawDelivery = getRawDelivery(productPageDocument, url);
		conn.debug("Raw delivery : " + rawDelivery);
		siteProduct.setRawDelivery(rawDelivery);

		int delivery = getDelivery(rawDelivery, url);
		conn.debug("Delivery : " + delivery);
		siteProduct.setDelivery(delivery);

		float deliveryPriceHome = getDeliveryPriceHome(productPageDocument, url);
		conn.debug("Delivery price home : " + deliveryPriceHome);
		siteProduct.setDeliveryPriceHome(deliveryPriceHome);

		handleRatingAndReviews(productPageDocument, siteProduct);

		return siteProduct;
	}

	private void handleRatingAndReviews(Document productPageDocument, SiteProduct siteProduct) {
		Integer ratingTotal = getRatingTotal(productPageDocument, siteProduct.getProductPath());
		conn.debug("Rating total : " + ratingTotal);
		siteProduct.setRatingTotal(ratingTotal);

		if (ratingTotal != null) {
			Integer ratingAverage = getRatingAverage(productPageDocument, siteProduct.getProductPath());
			conn.debug("Rating average : " + ratingAverage);
			siteProduct.setRatingAvg(ratingAverage);

			String ratingUrl = getRatingUrl(productPageDocument, siteProduct.getProductPath());
			conn.debug("Rating URL : " + ratingUrl);
			siteProduct.setRatingUrl(ratingUrl);

			handleRatingBands(productPageDocument, siteProduct);

			handleReviews(productPageDocument, siteProduct);
		}
	}

	private void handleReviews(final Element element, SiteProduct siteProduct) {
		Elements reviewsElements = element.select("Review Item selector ..."); // TODO
		List<SiteProductUserRating> userRatings = Lists.newArrayList();
		int reviewIndex = 0;
		for (Element reviewElement : reviewsElements) {
			reviewIndex++;
			showReview("Review :: " + reviewIndex);
			SiteProductUserRating userRating = new SiteProductUserRating();

			String reviewUser = getReviewUser(reviewElement, siteProduct.getRatingUrl());
			conn.debug("Review User : " + reviewUser);
			userRating.setUser(reviewUser);

			String reviewTitle = getReviewTitle(reviewElement, siteProduct.getRatingUrl());
			conn.debug("Review Title : " + reviewTitle);
			userRating.setTitle(reviewTitle);

			String reviewUrl = getReviewUrl(reviewElement, siteProduct.getRatingUrl());
			conn.debug("Review URL : " + reviewUrl);
			userRating.setUrl(reviewUrl);

			String reviewComment = getReviewComment(reviewElement, siteProduct.getRatingUrl());
			conn.debug("Review Comment : " + reviewComment);
			userRating.setComment(reviewComment);

			Integer reviewRating = getReviewRating(reviewElement, siteProduct.getRatingUrl());
			if (reviewRating == null) {
				continue;
			}
			conn.debug("Review Rating : " + reviewRating);
			userRating.setRating(reviewRating);

			Date reviewDate = getReviewDate(reviewElement, siteProduct.getRatingUrl());
			if (reviewDate == null) {
				continue;
			}
			conn.debug("Review Date : " + reviewDate);
			userRating.setReviewDate(reviewDate);

			userRatings.add(userRating);
		}
		siteProduct.addSiteProductUserRatings(userRatings);
	}

	private Date getReviewDate(final Element element, final String url) {
		final Element reviewDateElement = findElement(element, "Review Date selector ...");// TODO
		String reviewDateText = fromElementText(reviewDateElement);
		reviewDateText = validateField(reviewDateText, url, "Review Date", IEngineLogger.ERROR);
		return parseReviewDate(reviewDateText);
	}

	private Date parseReviewDate(final String reviewDateText) {
		String dateText = extractReviewDateText(reviewDateText);
		try {
			return LocalDateTime.parse(dateText, REVIEW_DATE_PARSER).toDate();
		} catch (Exception exc) {
			conn.error("Review date not parsable [" + dateText + "]" + withException(exc));
		}
		return null;
	}

	private String extractReviewDateText(String reviewDateText) {
		Matcher matcher = REVIEW_DATE_PATTERN.matcher(reviewDateText); // TODO
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	private Integer getReviewRating(final Element element, final String url) {
		final Element reviewRatingElement = findElement(element, "Review Rating selector ...");// TODO
		String reviewRatingText = fromElementText(reviewRatingElement);
		reviewRatingText = validateField(reviewRatingText, url, "Review Rating", IEngineLogger.ERROR);
		if (NumberUtils.isNumber(reviewRatingText)) {
			float reviewRating = Float.parseFloat(reviewRatingText) * 20;
			return (int) reviewRating;
		}
		return null;
	}

	private String getReviewComment(final Element element, final String url) {
		final Element reviewCommentElement = findElement(element, "Review Comment selector ...");// TODO
		String reviewComment = fromElementText(reviewCommentElement);
		return validateField(reviewComment, url, "Review Comment", IEngineLogger.WARN);
	}

	private String getReviewUrl(final Element element, final String url) {
		final Element reviewUrlElement = findElement(element, "Review URL selector ...");// TODO
		String reviewUrl = fromAbsoluteUrl(reviewUrlElement, "href");
		return validateField(reviewUrl, url, "Review URL", IEngineLogger.WARN);
	}

	private String getReviewTitle(final Element element, final String url) {
		final Element reviewTitleElement = findElement(element, "Review Title selector ...");// TODO
		String reviewTitle = fromElementText(reviewTitleElement);
		return validateField(reviewTitle, url, "Review Title", IEngineLogger.WARN);
	}

	private String getReviewUser(final Element element, final String url) {
		final Element reviewUserElement = findElement(element, "Review User selector ...");// TODO
		String reviewUser = fromElementText(reviewUserElement);
		return validateField(reviewUser, url, "Review User", IEngineLogger.WARN);
	}

	private void handleRatingBands(final Element element, SiteProduct siteProduct) {
		Integer ratingBand0to20 = getRatingBand(element, 1, siteProduct.getProductPath());
		conn.debug("Rating band 0 to 20 : " + ratingBand0to20);
		siteProduct.setRatingBand0to20(ratingBand0to20);

		Integer ratingBand20to40 = getRatingBand(element, 2, siteProduct.getProductPath());
		conn.debug("Rating band 20 to 40 : " + ratingBand20to40);
		siteProduct.setRatingBand20to40(ratingBand20to40);

		Integer ratingBand40to60 = getRatingBand(element, 3, siteProduct.getProductPath());
		conn.debug("Rating band 40 to 60 : " + ratingBand40to60);
		siteProduct.setRatingBand40to60(ratingBand40to60);

		Integer ratingBand60to80 = getRatingBand(element, 4, siteProduct.getProductPath());
		conn.debug("Rating band 60 to 80 : " + ratingBand60to80);
		siteProduct.setRatingBand60to80(ratingBand60to80);

		Integer ratingBand80to100 = getRatingBand(element, 5, siteProduct.getProductPath());
		conn.debug("Rating band 80 to 100 : " + ratingBand80to100);
		siteProduct.setRatingBand80to100(ratingBand80to100);
	}

	private Integer getRatingBand(final Element element, final int stars, final String url) {
		final Element ratingBandElement = findElement(element, "Rating band selector ...");// TODO Ex :
																							// "div.ratingBand[stars=" +
																							// stars + "]
		String ratingBandText = fromAttribute(ratingBandElement, "Rating band attribute ..."); // TODO
		ratingBandText = validateField(ratingBandText, url, "Rating band", IEngineLogger.DEBUG);
		if (StringUtils.isNumeric(ratingBandText)) {
			return Integer.parseInt(ratingBandText);
		}
		return null;
	}

	private String getRatingUrl(final Element element, final String url) {
		final Element ratingUrlElement = findElement(element, "Rating URL selector ...");// TODO
		String ratingUrl = fromAbsoluteUrl(ratingUrlElement, "href");
		return validateField(ratingUrl, url, "Rating URL", IEngineLogger.ERROR);
	}

	private Integer getRatingAverage(final Element element, final String url) {
		final Element ratingAverageElement = findElement(element, "Rating average selector ...");// TODO
		String ratingAverageText = fromAttribute(ratingAverageElement, "Rating average attribute ..."); // TODO
		ratingAverageText = validateField(ratingAverageText, url, "Rating average", IEngineLogger.ERROR);
		if (NumberUtils.isNumber(ratingAverageText)) {
			float ratingAverage = Float.parseFloat(ratingAverageText) * 20;
			return (int) ratingAverage;
		}
		return null;
	}

	private Integer getRatingTotal(final Element element, final String url) {
		Element ratingTotalElement = findElement(element, "Rating total selector ...");// TODO
		String ratingTotalText = fromElementText(ratingTotalElement);
		ratingTotalText = validateField(ratingTotalText, url, "Rating total", IEngineLogger.WARN);
		if (StringUtils.isNumeric(ratingTotalText)) {
			return Integer.parseInt(ratingTotalText);
		}
		return null;
	}

	private float getDeliveryPriceHome(final Element element, final String url) {
		final Element deliveryPriceElement = findElement(element, "Delivery price selector ..."); // TODO
		String deliveryPriceRaw = fromElementText(deliveryPriceElement);
		deliveryPriceRaw = validateField(deliveryPriceRaw, url, "Delivery price raw", IEngineLogger.WARN);
		Float deliveryPrice = parseFloat(deliveryPriceRaw);
		if (deliveryPrice == null) {
			return -1f;
		}
		return deliveryPrice;
	}

	private int getDelivery(final String rawDelivery, final String url) {
		if (StringUtils.isNotBlank(rawDelivery)) {
			final String lcRawDelivery = StringUtils.lowerCase(rawDelivery);
			if (isExpressedAsDate(lcRawDelivery)) {
				final LocalDate deliveryDate = parseDeliveryDate(lcRawDelivery);
				if (deliveryDate != null) {
					return Days.daysBetween(NOW, deliveryDate).getDays();
				}
				conn.error("Delivery date is null" + withUrl(url));
			}
			if (isExpressedAsPeriod(lcRawDelivery)) {
				final Period deliveryPeriod = parseDeliveryPeriod(lcRawDelivery);
				if (deliveryPeriod != null) {
					return Days.daysBetween(NOW, NOW.plus(deliveryPeriod)).getDays();
				}
				conn.error("Delivery period is null" + withUrl(url));
			}
		}
		return 0;
	}

	private LocalDate parseDeliveryDate(final String deliveryDateText) {
		String dateText = extractDeliveryDateText(deliveryDateText);
		try {
			return LocalDate.parse(dateText, DELIVERY_DATE_PARSER);
		} catch (Exception exc) {
			conn.error("Delivery date not parsable [" + dateText + "]" + withException(exc));
		}
		return null;
	}

	private Period parseDeliveryPeriod(final String deliveryPeriodText) {
		String periodText = extractPeriodText(deliveryPeriodText);
		try {
			return Period.parse(periodText, DELIVERY_PERIOD_PARSER);
		} catch (Exception exc) {
			conn.error("Delivery period not parsable [" + periodText + "]" + withException(exc));
		}
		return null;
	}

	private String extractDeliveryDateText(final String rawDelivery) {
		Matcher matcher = DELIVERY_DATE_PATTERN.matcher(rawDelivery); // TODO
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	private String extractPeriodText(final String rawDelivery) {
		Matcher matcher = DELIVERY_PERIOD_PATTERN.matcher(rawDelivery);// TODO
		if (matcher.find()) {
			return matcher.group();
		}
		return null;
	}

	private boolean isExpressedAsDate(final String rawDelivery) {
		// TODO
		return false;
	}

	private boolean isExpressedAsPeriod(final String rawDelivery) {
		// TODO
		return false;
	}

	private String getRawDelivery(final Element element, final String url) {
		Element deliveryElement = findElement(element, "Raw delivery selector ...");// TODO
		String rawDelivery = fromElementText(deliveryElement);
		return validateField(rawDelivery, url, "Raw delivery", IEngineLogger.WARN);
	}

	private String getEAN(final Element element, final String url) {
		final Element eanElement = findElement(element, "EAN selector ...");// TODO
		String ean = fromElementText(eanElement);
		ean = validateField(ean, url, "EAN", IEngineLogger.WARN);
		return isValidEAN(ean);
	}

	private String isValidEAN(final String ean) {
		if (StringUtils.isNotBlank(ean)) {
			final String checkedEan = checkEAN(ean);
			if (checkedEan.length() == 13 && EAN13CheckDigit.EAN13_CHECK_DIGIT.isValid(checkedEan)) {
				return checkedEan;
			}
			conn.warn("EAN not valid [" + checkedEan + "]. Set EAN to null!");
		}
		return null;
	}

	private String checkEAN(final String ean) {
		String exactEan = ean;
		if (exactEan.length() > 10) {
			exactEan = StringUtils.leftPad(exactEan, 13, CharUtils.toChar("0"));
		}
		final int length = exactEan.length();
		if (length > 13) {
			return StringUtils.substring(exactEan, length - 13, length);
		}
		return exactEan;
	}

	private String getMPN(final Element element, final String url) {
		Element mpnElement = findElement(element, "MPN selector ...");// TODO
		String mpn = fromElementText(mpnElement);
		mpn = validateField(mpn, url, "MPN", IEngineLogger.WARN);
		return isValidMPN(mpn);
	}

	private String isValidMPN(final String mpn) {
		if (StringUtils.isNotBlank(mpn)) {

			/**
			 * TODO : FILTRE A PERSONNALISER SELON LE SITE
			 * 
			 * Ces tests ne sont pas systematiques. Il faut d'abord s'assurer de l'intEgritE
			 * 
			 * de la valeur du MPN trouvE dans le site :
			 * 
			 * (recherche de la rEfErence via google,
			 * 
			 * verfication si certaines valeurs contiennent trop d'espaces, etc.)
			 * 
			 * Selon le cas, certains voire tous les filtres ci-dessous peuvent Etre omis du
			 * code.
			 *
			 */
			if (StringUtils.isNumeric(mpn)) {
				conn.debug("MPN consists only of digits and/or '-' [" + mpn + "]. Set MPN to null!");
				return null;
			}
			if (StringUtils.isAlpha(mpn)) {
				conn.debug("MPN consists only of letters and/or '-' [" + mpn + "]. Set MPN to null!");
				return null;
			}
			if (StringUtils.countMatches(mpn, " ") > 1) {
				conn.debug("MPN contains whitespace [" + mpn + "]. Set MPN to null!");
				return null;
			}
			if (mpn.length() < 4) {
				conn.debug("MPN is too short [" + mpn + "]. Set MPN to null!");
				return null;
			}
			return StringUtils.deleteWhitespace(mpn);
		}
		return null;
	}

	private String getSKU(final Element element, final String url) {
		final Element skuElement = findElement(element, "SKU selector ...");// TODO
		String sku = fromElementText(skuElement);
		return validateField(sku, url, "SKU", IEngineLogger.ERROR);
	}

	private String getBrand(final Element element, final String url) {
		final Element brandElement = findElement(element, "Brand selector ...");// TODO
		String brand = fromElementText(brandElement);
		return validateField(brand, url, "Brand", IEngineLogger.WARN);
	}

	private boolean getAvailability(final String availabilityText, final String url) {
		if (StringUtils.startsWithAny(availabilityText, "InStock", "LimitedStock", "PreOrder")) {// TODO
			return true;
		}
		if (StringUtils.startsWithAny(availabilityText, "OutOfStock", "SoldOut", "BackOrder")) {// TODO
			return false;
		}
		conn.error("New availability text found [" + availabilityText + "]" + withUrl(url));
		return false;
	}

	private String getAvailabilityText(final Element element, final String url) {
		final Element availabilityElement = findElement(element, "Availability text selector ...");// TODO
		String availabilityText = fromElementText(availabilityElement);
		return validateField(availabilityText, url, "Availability text", IEngineLogger.ERROR);
	}

	private String getSpecialPriceText(final Element element, final String url) {
		final Element specialPriceElement = findElement(element, "Special price text selector ...");// TODO
		String specialPriceText = fromElementText(specialPriceElement);
		return validateField(specialPriceText, url, "Special price text", IEngineLogger.WARN);
	}

	private Float getCashBackPrice(final Element element, final String url) {
		final Element priceElement = findElement(element, "CashBack price selector ...");// TODO
		String priceRaw = fromElementText(priceElement);
		priceRaw = validateField(priceRaw, url, "CashBack price", IEngineLogger.WARN);
		return parseFloat(priceRaw);
	}

	private Float getRecommendedRetailPrice(final Element element, final String url) {
		final Element priceElement = findElement(element, "Recommended retail price selector ...");// TODO
		String priceRaw = fromElementText(priceElement);
		priceRaw = validateField(priceRaw, url, "Recommended retail price", IEngineLogger.WARN);
		return parseFloat(priceRaw);
	}

	private Float getPreviousPriceFromSite(final Element element, final String url) {
		final Element priceElement = findElement(element, "Previous price from site selector ...");// TODO
		String priceRaw = fromElementText(priceElement);
		priceRaw = validateField(priceRaw, url, "Previous price from site", IEngineLogger.WARN);
		return parseFloat(priceRaw);
	}

	private float getPrice(final Element element, final String url) {
		final Element priceElement = findElement(element, "Price selector ...");// TODO
		String priceRaw = fromElementText(priceElement);
		priceRaw = validateField(priceRaw, url, "Price", IEngineLogger.ERROR);
		Float price = parseFloat(priceRaw);
		if (price == null) {
			return -1f;
		}
		return price;
	}

	/**
	 * Locale.FRENCH : e.g : 123 456 / 345 987,246
	 * 
	 * Locale.GERMANY : e.g : 123.456 / 345.987,246
	 * 
	 * Locale.US : e.g : 123,456 / 345,987.246
	 */
	private Float parseFloat(final String raw) {
		final String textNumber = CharMatcher.JAVA_DIGIT.or(CharMatcher.anyOf(".,")).retainFrom(raw);
		if (StringUtils.isNotBlank(textNumber)) {
			try {
				NumberFormat format = NumberFormat.getNumberInstance(CURRENT_LOCALE);
				Number number = format.parse(textNumber);
				return number.floatValue();
			} catch (ParseException exc) {
				conn.error("This text is not parsable to Float [" + textNumber + "]" + withException(exc));
			}
		}
		return null;
	}

	private String getImagePath(final Element element, final String url) {
		final Element imageElement = findElement(element, "Image path selector ...");// TODO
		String imagePath = fromAbsoluteUrl(imageElement, "src");
		return validateField(imagePath, url, "Image path", IEngineLogger.WARN);
	}

	private String getProductPath(final Element element, final String url) {
		final Element pathElement = findElement(element, "Product path selector ...");// TODO
		String productPath = fromAbsoluteUrl(pathElement, "href");
		return validateField(productPath, url, "Product path");
	}

	private String getTitle(final Element element, final String url) {
		final Element titleElement = findElement(element, "Title selector ..."); // TODO
		String title = fromElementText(titleElement);
		return validateField(title, url, "Title");
	}

	private Element findElement(final Element element, final String cssSelector) {
		if (element == null)
			return null;
		return element.select(cssSelector).first();
	}

	private String fromElementText(final Element element) {
		if (element != null) {
			String text = element.text();
			text = StringEscapeUtils.unescapeHtml4(text);
			text = text.replace(CARACTERE_ESPACE, " ");
			return StringUtils.trim(text);
		}
		return null;
	}

	private String fromAbsoluteUrl(final Element element, final String attr) {
		if (element != null) {
			String text = element.absUrl(attr);
			text = text.replace(CARACTERE_ESPACE, " ");
			return StringUtils.trim(text);
		}
		return null;
	}

	private String fromAttribute(final Element element, final String attr) {
		if (element != null) {
			String text = element.attr(attr);
			text = text.replace(CARACTERE_ESPACE, " ");
			return StringUtils.trim(text);
		}
		return null;
	}

	private String validateField(final String value, final String url, final String name) {
		if (StringUtils.isBlank(value)) {
			throw new NullPointerException(name + " not found" + withUrl(url));
		}
		return value;
	}

	private String validateField(final String value, final String url, final String name, final int log) {
		if (StringUtils.isBlank(value)) {
			conn.log(name + " not found" + withUrl(url), log);
			return StringUtils.EMPTY;
		}
		return value;
	}

	private String cleanPath(final String path) {
		return urlConsolidator.consolidateUrl(path);
	}

	private PageRequest getNextRequest(final Document listingPageDocument, final PageRequest previousRequest) {
		final Element nextPageElement = findElement(listingPageDocument, "Next page selector ...");// TODO
		String nextPageUrl = fromAbsoluteUrl(nextPageElement, "href");
		if (StringUtils.isNotBlank(nextPageUrl)) {
			return previousRequest.withUrl(nextPageUrl);
		}
		return null;
	}

	private boolean hasMorePages(final PageRequest request, final int loop) {
		if (loop > MAX_PAGE_LIMIT) {
			conn.error("Max page config attempt");
			return false;
		}
		return request != null;
	}

	private final class HttpMethod {
		public static final String GET = "GET";
		public static final String POST = "POST";
	}

	private class PageRequest {
		private String url;
		private String entity;
		private String method;
		private String cookie;

		public String getUrl() {
			return url;
		}

		public String getEntity() {
			return entity;
		}

		public String getMethod() {
			return method;
		}

		public String getCookie() {
			return cookie;
		}

		public PageRequest withUrl(String url) {
			this.url = url;
			return this;
		}

		public PageRequest withEntity(String entity) {
			this.entity = entity;
			return this;
		}

		public PageRequest withMethod(String method) {
			this.method = method;
			return this;
		}

		public PageRequest withCookie(String cookie) {
			this.cookie = cookie;
			return this;
		}
	}

	private class PageResponse {
		private int statusCode;
		private String content;
		private String location;
		private String setCookie;
		private boolean success;
		private boolean serverFailure;

		public int getStatusCode() {
			return statusCode;
		}

		public String getContent() {
			return content;
		}

		public String getLocation() {
			return location;
		}

		public String getSetCookie() {
			return setCookie;
		}

		public boolean isSuccess() {
			return success;
		}

		public boolean isServerFailure() {
			return serverFailure;
		}

		public void setStatusCode(int statusCode) {
			this.statusCode = statusCode;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public void setSetCookie(String setCookie) {
			this.setCookie = setCookie;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public void setServerFailure(boolean serverFailure) {
			this.serverFailure = serverFailure;
		}
	}

	private PageResponse connection(final PageRequest request, final String type) {
		conn.debug("Connect on " + type + " url : " + request.getUrl());
		try {
			PageResponse response = send(request);
			if (isSuccess(response)) {
				return response;
			}
			PageRequest newRequest = updateRequest(response, request);
			response = send(newRequest);
			int i = 1;
			while (!isSuccess(response) && i < MAX_PAGE_RETRY_COUNT) {
				newRequest = updateRequest(response, newRequest);
				response = send(newRequest);
				i++;
			}
			return response;
		} catch (InvalidPageException exc) {
			conn.warn("Failed to get page : '" + exc + "'");
		} catch (InvalidConnectionException exc) {
			conn.error("Connection parse error. '" + exc + "'");
		}
		return null;
	}

	private boolean isSuccess(PageResponse response) {
		return response != null && response.isSuccess();
	}

	private PageRequest updateRequest(final PageResponse response, final PageRequest defaultRequest) {
		if (response != null && response.getLocation() != null) {
			return defaultRequest.withUrl(response.getLocation());
		}
		return defaultRequest;
	}

	private PageResponse send(final PageRequest request) throws InvalidConnectionException, InvalidPageException {
		PageResponse pageResponse = null;
		try {
			switch (request.getMethod()) {
			case HttpMethod.GET:
				HttpGet getRequest = createHttpGet(allowHttpRedirection(false), request);
				HttpResponse getResponse = httpClient.execute(getRequest, context());
				pageResponse = responseHandler.handleResponse(getResponse);
				break;
			case HttpMethod.POST:
				HttpPost postRequest = createHttpPost(allowHttpRedirection(false), request);
				HttpResponse postResponse = httpClient.execute(postRequest, context());
				pageResponse = responseHandler.handleResponse(postResponse);
				break;
			default:
				conn.error("Method '" + request.getMethod() + "' is not supported!");
				break;
			}
		} catch (SocketTimeoutException exc) {
			conn.warn("Socket Time Out error" + withException(exc));
		} catch (SSLException exc) {
			conn.warn("SSL error" + withException(exc));
		} catch (ConnectionClosedException exc) {
			conn.warn("Connection prematurely closed" + withException(exc));
			pageResponse = new PageResponse();
			pageResponse.setServerFailure(true);
		} catch (SocketException exc) {
			throw new InvalidPageException(exc);
		} catch (Exception exc) {
			throw new InvalidConnectionException(exc);
		}
		if (pageResponse != null && pageResponse.isServerFailure()) {
			pause();
			reinitializeClient();
		}
		return pageResponse;
	}

	private HttpGet createHttpGet(HttpParams params, PageRequest request) {
		HttpGet httpGet = new HttpGet(request.getUrl());
		httpGet.addHeader("Accept", "*/*");
		httpGet.addHeader("Accept-Language", "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4"); // TODO
		httpGet.addHeader("Connection", "keep-alive");
		httpGet.addHeader("Cookie", request.getCookie());
		httpGet.addHeader("Host", host);
		httpGet.addHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
		httpGet.setParams(params);
		return httpGet;
	}

	private HttpPost createHttpPost(HttpParams params, PageRequest request) throws UnsupportedEncodingException {
		HttpPost httpPost = new HttpPost(request.getUrl());
		httpPost.addHeader("Accept", "*/*");
		httpPost.addHeader("Accept-Language", "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4"); // TODO
		httpPost.addHeader("Connection", "keep-alive");
		httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httpPost.addHeader("Cookie", request.getCookie());
		httpPost.addHeader("Host", host);
		httpPost.addHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36");
		httpPost.addHeader("X-Requested-With", "XMLHttpRequest");
		httpPost.setEntity(new StringEntity(request.getEntity(), HTTP.UTF_8));
		httpPost.setParams(params);
		return httpPost;
	}

	private void pause() {
		long millis = (long) (Math.random() * 3000);
		try {
			Thread.sleep(millis);
		} catch (InterruptedException iexc) {
			conn.error("Sleep error" + withException(iexc));
			Thread.currentThread().interrupt();
		}
	}

	private void reinitializeClient() {
		conn.debug("Reinitializing the client ...");
		httpClient.getConnectionManager().shutdown();
		httpClient = createClient();
	}

	private final ResponseHandler<PageResponse> responseHandler = new ResponseHandler<PageResponse>() {
		@Override
		public PageResponse handleResponse(HttpResponse httpResponse) throws IOException {
			PageResponse pageResponse = new PageResponse();
			pageResponse.setStatusCode(httpResponse.getStatusLine().getStatusCode());
			conn.debug("Status : " + pageResponse.getStatusCode());

			pageResponse.setSetCookie(getCookie(httpResponse.getHeaders("Set-Cookie")));
			conn.debug("Set-Cookie : " + pageResponse.getSetCookie());

			switch (pageResponse.getStatusCode()) {
			case 200: // Response OK
				final String content = getContent(httpResponse);
				pageResponse.setSuccess(true);
				pageResponse.setContent(content);
				break;
			case 301:
			case 302: // Redirected URL
				final String location = getLocation(httpResponse);
				pageResponse.setLocation(location);
				break;
			case 404: // Page not found
				EntityUtils.consume(httpResponse.getEntity());
				throw new SocketException("Page not found");
			default:
				pageResponse.setServerFailure(true);
				break;
			}
			EntityUtils.consume(httpResponse.getEntity());
			return pageResponse;
		}

		private String getLocation(HttpResponse response) {
			Header locationHeader = response.getFirstHeader("Location");
			if (locationHeader != null) {
				String location = locationHeader.getValue();
				return cleanPath(location);
			}
			return null;
		}

		private String getContent(HttpResponse response) throws IOException {
			Header contentEncodingHeader = response.getFirstHeader("Content-Encoding");
			if (contentEncodingHeader != null && contentEncodingHeader.getValue().contains("gzip")) {
				return StringUtils.trim(
						IOUtils.toString(new GzipDecompressingEntity(response.getEntity()).getContent(), HTTP.UTF_8));
			}
			return StringUtils.trim(EntityUtils.toString(response.getEntity(), HTTP.UTF_8));
		}

		private String getCookie(Header... headers) {
			List<String> list = Lists.newArrayList();
			for (Header header : headers) {
				list.add(StringUtils.substringBefore(header.getValue(), ";"));
			}
			return Joiner.on("; ").skipNulls().join(list);
		}
	};

	private HttpContext context() {
		CookieStore cookieStore = httpClient.getCookieStore();
		HttpContext localContext = new BasicHttpContext();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		return localContext;
	}

	private HttpParams allowHttpRedirection(boolean redirection) {
		HttpParams params = new BasicHttpParams();
		params.setParameter(ClientPNames.HANDLE_REDIRECTS, redirection);
		params.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);
		return params;
	}

	private String withUrl(String url) {
		return withSegmentId() + " - url : " + url;
	}

	private String withException(Exception exc) {
		return withSegmentId() + " - Exc : " + exc;
	}

	private String withSegmentId() {
		return ". segmentId : " + segment.getId();
	}

	@Override
	public EngineConnection startPass() {
		configureEngine();
		paginateThroughSegments();
		return null;
	}

	private void showReview(String message) {
		conn.debug("<div style='color: white; background-color: maroon; padding: 3px'>" + message + "</div>");
	}

	private void showOffer(final String message) {
		conn.debug("<div style='color: white; background-color: navy; padding: 3px'>" + message + "</div>");
	}

	private void showResult(final String message) {
		conn.debug("<div style='color: white; background-color: green; padding: 3px'>" + message + "</div>");
	}
}
