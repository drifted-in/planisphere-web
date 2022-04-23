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
import in.drifted.planisphere.renderer.html.HtmlRenderer;
import in.drifted.planisphere.renderer.svg.SvgRenderer;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

@ManagedBean
@ViewScoped
public class RenderBean implements Serializable, HttpSessionBindingListener {

    private static final String OUTPUT_FOLDER = "out";
    private static final List<String> SVG_URL_LIST = new LinkedList<>();
    private String htmlUrl;

    @ManagedProperty(value = "#{settingsBean}")
    private SettingsBean settingsBean;

    public void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        deleteObsoleteFiles();
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
    }

    public String renderSVG() {

        String outcome = null;

        storeOptions();

        deleteObsoleteFiles();

        try {
            Options options = settingsBean.getOptions();

            Path outputFolderPath = getOutputFolderPath();

            int count = options.isDoubleSided() ? 2 : 1;

            for (int i = 0; i < count; i++) {

                Path svgPath = Files.createTempFile(outputFolderPath, "planisphere", ".svg");

                try (
                        FileOutputStream outputStream = new FileOutputStream(svgPath.toFile());
                        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                    if (i == 1) {
                        options = new Options(options.getLatitudeFixed(), options.getLocale(), options.getThemeScreen(), options.getThemePrint(), -1,
                                options.hasConstellationLines(), options.hasConstellationLabels(), options.getConstellationLabelsMode(),
                                options.hasConstellationBoundaries(), options.hasMilkyWay(), options.hasDayLightSavingTimeScale(),
                                options.hasCoordsRADec(), options.hasEcliptic(), options.hasAllVisibleStars());
                    }

                    String themeScreen = options.getThemeScreen();
                    SvgRenderer.createFromTemplate(themeScreen.split("_")[0], themeScreen, output, options);

                    outputStream.write(output.toByteArray());
                }

                SVG_URL_LIST.add(OUTPUT_FOLDER + "/" + svgPath.getFileName());
            }

            outcome = "index.xhtml?faces-redirect=true";

        } catch (IOException e) {
            // Faces message
        }

        return outcome;
    }

    public String renderHTML() {

        String outcome = null;

        storeOptions();

        Options storedOptions = settingsBean.getOptions();

        if (htmlUrl != null) {
            try {
                Files.deleteIfExists(Paths.get(getRealPath(htmlUrl)));

            } catch (IOException e) {
                // this one doesn't harm, just log
            }
        }

        try {
            Path htmlPath = Files.createTempFile(getOutputFolderPath(), "planisphere", ".html");

            HtmlRenderer.createFromTemplate(storedOptions, htmlPath);
            htmlUrl = OUTPUT_FOLDER + "/" + htmlPath.getFileName();

            outcome = htmlUrl + "?faces-redirect=true";

        } catch (IOException e) {
            // Faces mesasage
        }

        return outcome;
    }

    private void storeOptions() {
        if (settingsBean != null) {
            settingsBean.storeOptions();
        }
    }

    private String getRealPath(String path) {
        ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        return context.getRealPath(path);
    }

    private Path getOutputFolderPath() throws IOException {
        Path outputFolderPath = Paths.get(getRealPath(OUTPUT_FOLDER));
        if (Files.notExists(outputFolderPath)) {
            Files.createDirectory(outputFolderPath);
        }
        return outputFolderPath;
    }

    public List<String> getSvgUrlList() {
        if (SVG_URL_LIST.isEmpty()) {
            renderSVG();
        }
        return SVG_URL_LIST;
    }

    private void deleteObsoleteFiles() {
        try {
            for (String url : SVG_URL_LIST) {
                Files.deleteIfExists(Paths.get(getRealPath(url)));
            }
        } catch (IOException e) {
            // Current files couldn't be deleted
        }
        SVG_URL_LIST.clear();
    }

}
