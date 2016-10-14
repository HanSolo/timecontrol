/*
 * Copyright (c) 2016 by Gerrit Grunwald
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
 */

package eu.hansolo.fx.timecontrol.fonts;

import javafx.scene.text.Font;


/**
 * User: hansolo
 * Date: 01.10.16
 * Time: 04:47
 */
public class Fonts {
    private static final String ROBOTO_LIGHT_NAME;
    private static final String ROBOTO_REGULAR_NAME;

    private static String robotoLightName;
    private static String robotoRegularName;


    static {
        try {
            robotoLightName   = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/timecontrol/fonts/Roboto-Light.ttf"), 10).getName();
            robotoRegularName = Font.loadFont(Fonts.class.getResourceAsStream("/eu/hansolo/fx/timecontrol/fonts/Roboto-Regular.ttf"), 10).getName();
        } catch (Exception exception) { }
        ROBOTO_LIGHT_NAME     = robotoLightName;
        ROBOTO_REGULAR_NAME   = robotoRegularName;
    }


    // ******************** Methods *******************************************
    public static Font robotoLight(final double SIZE) { return new Font(ROBOTO_LIGHT_NAME, SIZE); }
    public static Font robotoRegular(final double SIZE) { return new Font(ROBOTO_REGULAR_NAME, SIZE); }
}
