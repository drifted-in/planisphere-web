/*
 * Copyright (c) 2012 Jan Tošovský <jan.tosovsky.cz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package in.drifted.planisphere.web;

import in.drifted.planisphere.Options;
import in.drifted.planisphere.Settings;
import in.drifted.planisphere.l10n.LocalizationUtil;
import in.drifted.planisphere.util.LanguageUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;

@ManagedBean
@SessionScoped
public class SettingsBean implements Serializable {

    private static final List<Integer> LATITUDE_LIST = new ArrayList<>();
    private static final Map<String, Locale> LOCALE_MAP = new TreeMap<>();
    private static final List<String> THEME_SCREEN_LIST = new ArrayList<>();
    private static final List<String> THEME_PRINT_LIST = new ArrayList<>();
    private static final List<Integer> CONSTELLATION_LABEL_MODE_LIST = new ArrayList<>();
    private static final Map<String, Object> COOKIE_PROPERTIES_MAP = new HashMap<>();

    private final String googleAnalyticsId;

    private Integer latitude;
    private Locale locale;
    private String localeValue;
    private String direction;
    private Integer themeScreenIndex;
    private Integer themePrintIndex;
    private Boolean constellationLines;
    private Boolean constellationLabels;
    private Integer constellationLabelsMode;
    private Boolean constellationBoundaries;
    private Boolean milkyWay;
    private Boolean dayLightSavingTimeScale;
    private Boolean coordsRADec;
    private Boolean ecliptic;
    private Boolean allVisibleStars;

    private Options options;

    public SettingsBean() throws IOException {

        createLatitudeList();
        createLocaleMap();
        createThemeList("screen");
        createThemeList("print");
        createConstellationLabelModeList();

        COOKIE_PROPERTIES_MAP.clear();
        COOKIE_PROPERTIES_MAP.put("maxAge", 60 * 60 * 24 * 365);
        COOKIE_PROPERTIES_MAP.put("path", "/");

        googleAnalyticsId = loadGoogleAnalyticsId();
    }

    @PostConstruct
    public void init() {
        locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        options = getCookieOptions();
        loadOptions(options);
    }

    private Options getCookieOptions() {

        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        Map<String, Object> cookieMap = context.getRequestCookieMap();

        Locale cookieLocale = locale;

        Cookie cookie = (Cookie) cookieMap.get("localeValue");
        if (cookie != null && cookie.getValue() != null) {
            cookieLocale = getSupportedLocale(Locale.forLanguageTag(cookie.getValue()));
        }

        for (Map.Entry<String, Locale> entry : LOCALE_MAP.entrySet()) {
            if (cookieLocale == entry.getValue()) {
                localeValue = entry.getKey();
                break;
            }
        }

        double cookieLatitude = 50;

        cookie = (Cookie) cookieMap.get("latitude");
        if (cookie != null && cookie.getValue() != null) {
            try {
                double temp = Double.parseDouble(cookie.getValue());
                if (temp > -90 && temp < 90) {
                    cookieLatitude = temp;
                }
            } catch (NumberFormatException e) {
            }
        }

        return new Options(cookieLatitude, cookieLocale, Settings.THEME_PRINT_DEFAULT);
    }

    private void loadOptions(Options options) {

        latitude = (int) options.getLatitude();
        locale = options.getLocale();
        themeScreenIndex = getThemePageNumber(THEME_SCREEN_LIST, options.getThemeScreen(), Settings.TEMPLATE_SCREEN_DEFAULT);
        themePrintIndex = getThemePageNumber(THEME_PRINT_LIST, options.getThemePrint(), Settings.TEMPLATE_PRINT_DEFAULT);
        constellationLines = options.hasConstellationLines();
        constellationLabels = options.hasConstellationLabels();
        constellationLabelsMode = options.getConstellationLabelsMode();
        constellationBoundaries = options.hasConstellationBoundaries();
        milkyWay = options.hasMilkyWay();
        dayLightSavingTimeScale = options.hasDayLightSavingTimeScale();
        coordsRADec = options.hasCoordsRADec();
        ecliptic = options.hasEcliptic();
        allVisibleStars = options.hasAllVisibleStars();

        updateDirection();
    }

    public void storeOptions() {

        String themeScreen = THEME_SCREEN_LIST.get(themeScreenIndex);
        String themePrint = THEME_PRINT_LIST.get(themePrintIndex);

        options = new Options(latitude, locale, themeScreen, themePrint, 1, constellationLines,
                constellationLabels, constellationLabelsMode, constellationBoundaries, milkyWay,
                dayLightSavingTimeScale, coordsRADec, ecliptic, allVisibleStars);

        createLongLivedCookies();
    }

    private void createLongLivedCookies() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.addResponseCookie("localeValue", options.getLocale().toLanguageTag(), COOKIE_PROPERTIES_MAP);
        context.addResponseCookie("latitude", String.valueOf(options.getLatitude()), COOKIE_PROPERTIES_MAP);
    }

    public String cancelSettings() {
        loadOptions(options);
        return "index.xhtml?faces-redirect=true";
    }

    private static void createLatitudeList() {
        LATITUDE_LIST.clear();
        for (Integer i = 80; i >= 0; i = i - 5) {
            LATITUDE_LIST.add(i);
        }
        for (Integer i = -5; i >= -80; i = i - 5) {
            LATITUDE_LIST.add(i);
        }
    }

    private static void createLocaleMap() {

        Iterator<Locale> supportedLocaleIterator = FacesContext.getCurrentInstance().getApplication().getSupportedLocales();

        LOCALE_MAP.clear();
        while (supportedLocaleIterator.hasNext()) {
            Locale supportedLocale = supportedLocaleIterator.next();
            LocalizationUtil l = new LocalizationUtil(supportedLocale);
            LOCALE_MAP.put(l.getValue("localization"), supportedLocale);
        }
    }

    private static void createThemeList(String media) throws IOException {

        Collection<String> colorSchemeCollection = new HashSet<>();

        for (String templateName : Settings.getTemplateNameCollection(media)) {
            colorSchemeCollection.addAll(Settings.getColorSchemeCollection(templateName));
        }
        List<String> themeList = new ArrayList<>(colorSchemeCollection);
        Collections.sort(themeList);

        if (media.equals(Settings.MEDIA_SCREEN)) {
            THEME_SCREEN_LIST.clear();
            THEME_SCREEN_LIST.addAll(themeList);
        } else {
            THEME_PRINT_LIST.clear();
            THEME_PRINT_LIST.addAll(themeList);
        }
    }

    private static int getThemePageNumber(List<String> themeList, String theme, String defaultTheme) {
        if (!themeList.contains(theme)) {
            theme = defaultTheme;
        }
        for (int i = 0; i < themeList.size(); i++) {
            if (themeList.get(i).equals(theme)) {
                return i;
            }
        }
        return 1;
    }

    private static void createConstellationLabelModeList() {
        CONSTELLATION_LABEL_MODE_LIST.clear();
        for (Integer i = 0; i >= 2; i++) {
            CONSTELLATION_LABEL_MODE_LIST.add(i);
        }
    }

    private static Locale getSupportedLocale(Locale locale) {
        String localeLanguage = locale.getLanguage();
        for (Map.Entry<String, Locale> entry : LOCALE_MAP.entrySet()) {
            String entryLanguage = entry.getValue().getLanguage();
            if (entryLanguage.equals(localeLanguage)) {
                return entry.getValue();
            }
        }
        return Locale.ENGLISH;
    }

    private static String loadGoogleAnalyticsId() {
        try ( InputStream inputStream = SettingsBean.class.getResourceAsStream("/google-analytics.properties")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("id");

        } catch (IOException e) {
        }
        return null;
    }

    private void updateDirection() {
        direction = LanguageUtil.getWritingDirection(locale.getLanguage());
    }

    public void localeValueChanged() {
        locale = LOCALE_MAP.get(localeValue);
        updateDirection();
    }

    public List<Integer> getLatitudeList() {
        return LATITUDE_LIST;
    }

    public List<String> getThemePrintList() {
        return THEME_PRINT_LIST;
    }

    public List<String> getThemeScreenList() {
        return THEME_SCREEN_LIST;
    }

    public List<Integer> getConstellationLabelsOptionsList() {
        return CONSTELLATION_LABEL_MODE_LIST;
    }

    public String getGoogleAnalyticsId() {
        return googleAnalyticsId;
    }

    public Integer getLatitude() {
        return latitude;
    }

    public void setLatitude(Integer latitude) {
        this.latitude = latitude;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLocaleValue() {
        return localeValue;
    }

    public void setLocaleValue(String localeValue) {
        this.localeValue = localeValue;
    }

    public List<String> getLocaleValueList() {
        return new ArrayList<String>(LOCALE_MAP.keySet());
    }

    public Integer getThemeScreenIndex() {
        return themeScreenIndex;
    }

    public void setThemeScreenIndex(Integer themeScreenIndex) {
        this.themeScreenIndex = themeScreenIndex;
    }

    public Integer getThemePrintIndex() {
        return themePrintIndex;
    }

    public void setThemePrintIndex(Integer themePrintIndex) {
        this.themePrintIndex = themePrintIndex;
    }

    public Boolean getConstellationLines() {
        return constellationLines;
    }

    public void setConstellationLines(Boolean constellationLines) {
        this.constellationLines = constellationLines;
    }

    public Boolean getConstellationLabels() {
        return constellationLabels;
    }

    public void setConstellationLabels(Boolean constellationLabels) {
        this.constellationLabels = constellationLabels;
    }

    public Integer getConstellationLabelsMode() {
        return constellationLabelsMode;
    }

    public void setConstellationLabelsMode(Integer constellationLabelsMode) {
        this.constellationLabelsMode = constellationLabelsMode;
    }

    public Boolean getConstellationBoundaries() {
        return constellationBoundaries;
    }

    public void setConstellationBoundaries(Boolean constellationBoundaries) {
        this.constellationBoundaries = constellationBoundaries;
    }

    public Boolean getMilkyWay() {
        return milkyWay;
    }

    public void setMilkyWay(Boolean milkyWay) {
        this.milkyWay = milkyWay;
    }

    public Boolean getDayLightSavingTimeScale() {
        return dayLightSavingTimeScale;
    }

    public void setDayLightSavingTimeScale(Boolean dayLightSavingTimeScale) {
        this.dayLightSavingTimeScale = dayLightSavingTimeScale;
    }

    public Boolean getCoordsRADec() {
        return coordsRADec;
    }

    public void setCoordsRADec(Boolean coordsRADec) {
        this.coordsRADec = coordsRADec;
    }

    public Boolean getEcliptic() {
        return ecliptic;
    }

    public void setEcliptic(Boolean ecliptic) {
        this.ecliptic = ecliptic;
    }

    public Boolean getAllVisibleStars() {
        return allVisibleStars;
    }

    public void setAllVisibleStars(Boolean allVisibleStars) {
        this.allVisibleStars = allVisibleStars;
    }

    public String getDirection() {
        return direction;
    }

    public Options getOptions() {
        return options;
    }

}
