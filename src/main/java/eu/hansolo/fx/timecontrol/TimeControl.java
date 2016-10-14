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

package eu.hansolo.fx.timecontrol;

import eu.hansolo.fx.timecontrol.fonts.Fonts;
import javafx.beans.DefaultProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.sql.Time;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


/**
 * User: hansolo
 * Date: 13.10.16
 * Time: 15:04
 */
@DefaultProperty("children")
public class TimeControl extends Region {
    private enum TickLabelOrientation { ORTHOGONAL,  HORIZONTAL, TANGENT }
    private enum TouchPointType { START, STOP }
    private static final DateTimeFormatter         TIME_FORMAT      = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter         HOUR_FORMAT      = DateTimeFormatter.ofPattern("HH");
    private static final DateTimeFormatter         MINUTE_FORMAT    = DateTimeFormatter.ofPattern("mm");
    private static final double                    ANGLE_STEP       = 0.00416667; // degrees per seconds a day
    private static final double                    PREFERRED_WIDTH  = 400;
    private static final double                    PREFERRED_HEIGHT = 505;
    private static final double                    MINIMUM_WIDTH    = 40;
    private static final double                    MINIMUM_HEIGHT   = 50;
    private static final double                    MAXIMUM_WIDTH    = 1024;
    private static final double                    MAXIMUM_HEIGHT   = 1024;
    private static final double                    ASPECT_RATIO     = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private              double                    size;
    private              double                    width;
    private              double                    height;
    private              Text                      startText;
    private              Text                      startTimeText;
    private              Text                      stopText;
    private              Text                      stopTimeText;
    private              Text                      hourText;
    private              Text                      hourUnitText;
    private              Text                      minuteText;
    private              Text                      minuteUnitText;
    private              HBox                      durationBox;
    private              Canvas                    canvas;
    private              Arc                       barBackground;
    private              Arc                       bar;
    private              Rotate                    barRotate;
    private              Circle                    touchPointStart;
    private              Circle                    touchPointStop;
    private              Rotate                    touchRotate;
    private              double                    iconSize;
    private              Region                    startIcon;
    private              Region                    stopIcon;
    private              Region                    startPointIcon;
    private              Region                    stopPointIcon;
    private              GraphicsContext           ctx;
    private              Pane                      pane;
    private              double                    mouseScaleX;
    private              double                    mouseScaleY;
    private              ObjectProperty<Color>     barBackgroundColor;
    private              ObjectProperty<Color>     barColor;
    private              ObjectProperty<Color>     backgroundColor;
    private              ObjectProperty<Color>     textColor;
    private              ObjectProperty<Duration>  duration;
    private              Paint                     backgroundPaint;
    private              Paint                     borderPaint;
    private              double                    borderWidth;
    private              ObjectProperty<LocalTime> startTime;
    private              ObjectProperty<LocalTime> stopTime;



    // ******************** Constructors **************************************
    public TimeControl() {
        getStylesheets().add(TimeControl.class.getResource("timecontrol.css").toExternalForm());
        barBackgroundColor = new ObjectPropertyBase<Color>(Color.web("#171717")) {
            @Override protected void invalidated() { resize(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "barBackgroundColor"; }
        };
        barColor           = new ObjectPropertyBase<Color>(Color.web("#ffb500")) {
            @Override protected void invalidated() { resize(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "barColor"; }
        };
        backgroundColor    = new ObjectPropertyBase<Color>(Color.web("#0d0d0d")) {
            @Override protected void invalidated() { backgroundPaint = get(); redraw(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "backgroundColor"; }
        };
        textColor          = new ObjectPropertyBase<Color>(Color.WHITE) {
            @Override protected void invalidated() { resize(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "textColor"; }
        };
        iconSize           = 40;
        backgroundPaint    = Color.web("#0d0d0d");
        borderPaint        = Color.TRANSPARENT;
        borderWidth        = 0d;
        mouseScaleX        = 1.0;
        mouseScaleY        = 1.0;
        startTime          = new ObjectPropertyBase<LocalTime>(LocalTime.of(0, 0)) {
            @Override protected void invalidated() { updateBar(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "startTime"; }
        };
        stopTime           = new ObjectPropertyBase<LocalTime>(LocalTime.of(0, 0)) {
            @Override protected void invalidated() { updateBar(); }
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "stopTime"; }
        };
        duration           = new ObjectPropertyBase<Duration>(Duration.between(getStopTime(), getStartTime())) {
            @Override public Object getBean() { return TimeControl.this; }
            @Override public String getName() { return "duration"; }
        };

        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        getStyleClass().add("time-control");

        startIcon = new Region();
        startIcon.getStyleClass().add("start-icon");

        stopIcon = new Region();
        stopIcon.getStyleClass().add("stop-icon");

        startText = new Text("Start");
        startText.setTextOrigin(VPos.TOP);

        startTimeText = new Text("00:00");
        startTimeText.setTextOrigin(VPos.TOP);

        stopText = new Text("Stop");
        stopText.setTextOrigin(VPos.TOP);

        stopTimeText = new Text("00:00");
        stopTimeText.setTextOrigin(VPos.TOP);

        hourText = new Text("0");
        hourText.setTextOrigin(VPos.CENTER);

        hourUnitText = new Text("h");
        hourUnitText.setTextOrigin(VPos.BOTTOM);

        minuteText = new Text("0");
        minuteText.setTextOrigin(VPos.CENTER);

        minuteUnitText = new Text("m");
        minuteUnitText.setTextOrigin(VPos.BOTTOM);

        durationBox = new HBox(hourText, hourUnitText, minuteText, minuteUnitText);
        durationBox.setAlignment(Pos.BASELINE_CENTER);

        canvas = new Canvas(400, 400);
        ctx    = canvas.getGraphicsContext2D();

        barBackground = new Arc(200, 350, 200, 200, 0, 360);
        barBackground.setStrokeLineCap(StrokeLineCap.BUTT);
        barBackground.setType(ArcType.OPEN);
        barBackground.setFill(null);

        barRotate = new Rotate(-90, PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.6039604);

        bar = new Arc(200, 350, 200, 200, 0, 0);
        bar.setStrokeLineCap(StrokeLineCap.ROUND);
        bar.setType(ArcType.OPEN);
        bar.setFill(null);
        bar.getTransforms().add(barRotate);

        touchRotate = new Rotate(-180, PREFERRED_WIDTH * 0.5, PREFERRED_HEIGHT * 0.6039604);

        touchPointStart = new Circle();
        touchPointStart.getTransforms().add(touchRotate);

        touchPointStop  = new Circle();
        touchPointStop.getTransforms().add(touchRotate);

        startPointIcon = new Region();
        startPointIcon.getStyleClass().add("start-icon");
        startPointIcon.setMouseTransparent(true);

        stopPointIcon = new Region();
        stopPointIcon.getStyleClass().add("stop-icon");
        stopPointIcon.setMouseTransparent(true);

        pane = new Pane(startIcon, startText, startTimeText, stopIcon, stopText, stopTimeText, durationBox, canvas, barBackground, bar, touchPointStart, touchPointStop, startPointIcon, stopPointIcon);
        pane.setBackground(new Background(new BackgroundFill(backgroundPaint, CornerRadii.EMPTY, Insets.EMPTY)));
        pane.setBorder(new Border(new BorderStroke(borderPaint, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(borderWidth))));

        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        touchPointStart.setOnMouseDragged(evt -> touchRotate(evt.getSceneX() * mouseScaleX, evt.getSceneY() * mouseScaleY, TouchPointType.START));
        touchPointStop.setOnMouseDragged(evt -> touchRotate(evt.getSceneX() * mouseScaleX, evt.getSceneY() * mouseScaleY, TouchPointType.STOP));
    }


    // ******************** Methods *******************************************
    @Override protected double computeMinWidth(final double HEIGHT) { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double WIDTH) { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double HEIGHT) { return super.computePrefWidth(HEIGHT); }
    @Override protected double computePrefHeight(final double WIDTH) { return super.computePrefHeight(WIDTH); }
    @Override protected double computeMaxWidth(final double HEIGHT) { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double WIDTH) { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public Color getBackgroundColor() { return backgroundColor.get(); }
    public void setBackgroundColor(final Color COLOR) { backgroundColor.set(COLOR); }
    public ObjectProperty<Color> backgroundColorProperty() { return backgroundColor; }

    public Color getBarBackgroundColor() { return barBackgroundColor.get(); }
    public void setBarBackgroundColor(final Color COLOR) { barBackgroundColor.set(COLOR); }
    public ObjectProperty<Color> barBackgroundColorProperty() { return barBackgroundColor; }

    public Color getBarColor() { return barColor.get(); }
    public void setBarColor(final Color COLOR) { barColor.set(COLOR); }
    public ObjectProperty<Color> barColorProperty() { return barColor; }

    public Color getTextColor() { return textColor.get(); }
    public void setTextColor(final Color COLOR) { textColor.set(COLOR); }
    public ObjectProperty<Color> textColorProperty() { return textColor; }

    public LocalTime getStartTime() { return startTime.get(); }
    public void setStartTime(final LocalTime TIME) { startTime.set(TIME); }
    public ObjectProperty<LocalTime> startTimeProperty() { return startTime; }

    public LocalTime getStopTime() { return stopTime.get(); }
    public void setStopTime(final LocalTime TIME) { stopTime.set(TIME); }
    public ObjectProperty<LocalTime> stopTimeProperty() { return stopTime; }

    public Duration getDuration() { return duration.get(); }
    public ReadOnlyObjectProperty<Duration> durationProperty() { return duration; }


    private void drawTickmarks() {
        double  sinValue;
        double  cosValue;
        double  ctxSize            = canvas.getWidth();
        double  startAngle         = 180;
        double  angleStep          = 3.75;
        Point2D center             = new Point2D(ctxSize * 0.5, ctxSize * 0.5);
        Color   minorTickMarkColor = Color.color(getTextColor().getRed(), getTextColor().getGreen(), getTextColor().getBlue(), 0.25);
        Color   majorTickMarkColor = Color.color(getTextColor().getRed(), getTextColor().getGreen(), getTextColor().getBlue(), 0.5);
        Font    font               = Fonts.robotoLight(width * 0.04);
        ctx.clearRect(0, 0, ctxSize, ctxSize);
        ctx.setLineCap(StrokeLineCap.BUTT);
        ctx.setFont(font);
        ctx.setLineWidth(ctxSize * 0.005);
        for (double angle = 0, counter = 0 ; Double.compare(counter, 95) <= 0 ; angle -= angleStep, counter++) {
            sinValue = Math.sin(Math.toRadians(angle + startAngle));
            cosValue = Math.cos(Math.toRadians(angle + startAngle));

            Point2D innerPoint       = new Point2D(center.getX() + ctxSize * 0.465 * sinValue, center.getY() + ctxSize * 0.465 * cosValue);
            Point2D innerMinutePoint = new Point2D(center.getX() + ctxSize * 0.478 * sinValue, center.getY() + ctxSize * 0.478 * cosValue);
            Point2D outerPoint       = new Point2D(center.getX() + ctxSize * 0.5 * sinValue, center.getY() + ctxSize * 0.5 * cosValue);
            Point2D textPoint        = new Point2D(center.getX() + ctxSize * 0.42 * sinValue, center.getY() + ctxSize * 0.42 * cosValue);

            if (counter % 4 == 0) {
                ctx.setStroke(majorTickMarkColor);
                ctx.strokeLine(innerPoint.getX(), innerPoint.getY(), outerPoint.getX(), outerPoint.getY());
                ctx.save();
                ctx.translate(textPoint.getX(), textPoint.getY());

                rotateContextForText(ctx, startAngle, angle, TickLabelOrientation.HORIZONTAL);
                ctx.setTextAlign(TextAlignment.CENTER);
                ctx.setTextBaseline(VPos.CENTER);
                ctx.setFill(majorTickMarkColor);
                ctx.fillText(counter == 0 ? "0" : Integer.toString((int) (counter / 4)), 0, 0);
                ctx.restore();
            } else if (counter % 1 == 0) {
                ctx.setStroke(minorTickMarkColor);
                ctx.strokeLine(innerMinutePoint.getX(), innerMinutePoint.getY(), outerPoint.getX(), outerPoint.getY());
            }
        }
    }

    private void rotateContextForText(final GraphicsContext CTX, final double START_ANGLE, final double ANGLE, final TickLabelOrientation ORIENTATION) {
        switch (ORIENTATION) {
            case ORTHOGONAL:
                if ((360 - START_ANGLE - ANGLE) % 360 > 90 && (360 - START_ANGLE - ANGLE) % 360 < 270) {
                    CTX.rotate((180 - START_ANGLE - ANGLE) % 360);
                } else {
                    CTX.rotate((360 - START_ANGLE - ANGLE) % 360);
                }
                break;
            case TANGENT:
                if ((360 - START_ANGLE - ANGLE - 90) % 360 > 90 && (360 - START_ANGLE - ANGLE - 90) % 360 < 270) {
                    CTX.rotate((90 - START_ANGLE - ANGLE) % 360);
                } else {
                    CTX.rotate((270 - START_ANGLE - ANGLE) % 360);
                }
                break;
            case HORIZONTAL:
            default:
                break;
        }
    }

    private void touchRotate(final double X, final double Y, final TouchPointType TYPE) {
        double theta           = getTheta(X, Y);
        double angle           = -theta % 360;
        double touchPointAngle = Math.toRadians(-theta - 90);
        if (TouchPointType.START == TYPE) {
            touchPointStart.setCenterX(bar.getCenterX() + bar.getRadiusX() * Math.sin(touchPointAngle));
            touchPointStart.setCenterY(bar.getCenterY() + bar.getRadiusY() * Math.cos(touchPointAngle));
            startPointIcon.resize(iconSize, iconSize);
            startPointIcon.relocate(touchPointStart.getBoundsInParent().getMinX() + iconSize * 0.6, touchPointStart.getBoundsInParent().getMinY() + iconSize * 0.6);
            setStartTime(LocalTime.ofSecondOfDay((long) Math.abs(angle / ANGLE_STEP)).plusSeconds(21600));
        } else {
            touchPointStop.setCenterX(bar.getCenterX() + bar.getRadiusX() * Math.sin(touchPointAngle));
            touchPointStop.setCenterY(bar.getCenterY() + bar.getRadiusY() * Math.cos(touchPointAngle));
            stopPointIcon.resize(iconSize, iconSize);
            stopPointIcon.relocate(touchPointStop.getBoundsInParent().getMinX() + iconSize * 0.6, touchPointStop.getBoundsInParent().getMinY() + iconSize * 0.6);
            setStopTime(LocalTime.ofSecondOfDay((long) Math.abs(angle / ANGLE_STEP)).plusSeconds(21600));
        }
    }

    private double getTheta(double x, double y) {
        double deltaX = x - (width * 0.5);
        double deltaY = y - (height * 0.6039604);
        double radius = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
        double nx     = deltaX / radius;
        double ny     = deltaY / radius;
        double theta  = Math.atan2(ny, nx);
        return Double.compare(theta, 0.0) >= 0 ? Math.toDegrees(theta) : Math.toDegrees((theta)) + 360.0;
    }

    private void updateBar() {
        int stopPointSeconds  = getStopTime().toSecondOfDay();
        int startPointSeconds = getStartTime().toSecondOfDay() > stopPointSeconds ? getStartTime().toSecondOfDay() - 86400 : getStartTime().toSecondOfDay();
        int deltaSeconds      = Math.abs(stopPointSeconds - startPointSeconds);
        duration.set(Duration.ofSeconds(deltaSeconds));

        bar.setStartAngle(-startPointSeconds * ANGLE_STEP);
        bar.setLength(-deltaSeconds * ANGLE_STEP);

        startTimeText.setText(TIME_FORMAT.format(getStartTime()));
        stopTimeText.setText(TIME_FORMAT.format(getStopTime()));

        hourText.setText(HOUR_FORMAT.format((getStopTime().minusSeconds(startPointSeconds))));
        minuteText.setText(MINUTE_FORMAT.format((getStopTime().minusSeconds(startPointSeconds))));
        durationBox.setLayoutX((width - durationBox.getLayoutBounds().getWidth()) * 0.5);
    }


    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size   = width < height ? width : height;

        if (ASPECT_RATIO * width > height) {
            width = 1 / (ASPECT_RATIO / height);
        } else if (1 / (ASPECT_RATIO / height) > width) {
            height = ASPECT_RATIO * width;
        }

        if (width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);
            pane.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            if (Double.compare(pane.getLayoutBounds().getWidth(), 0) == 0) {
                mouseScaleX = 1.0;
                mouseScaleY = 1.0;
            } else {
                mouseScaleX = pane.getLayoutBounds().getWidth() / getWidth();
                mouseScaleY = pane.getLayoutBounds().getHeight() / getHeight();
            }

            iconSize = width * 0.04725;

            startIcon.setPrefSize(iconSize, iconSize);
            startIcon.relocate(0, width * 0.022);

            startText.setFont(Fonts.robotoRegular(width * 0.05));
            startText.setX(width * 0.065);
            startText.setY(width * 0.0125);

            startTimeText.setFont(Fonts.robotoRegular(width * 0.12));
            startTimeText.setX(width * 0.05);
            startTimeText.setY(width * 0.0825);

            stopText.setFont(Fonts.robotoRegular(width * 0.05));
            stopText.setX(width - stopText.getLayoutBounds().getWidth());
            stopText.setY(width * 0.0125);

            stopIcon.setPrefSize(iconSize, iconSize);
            stopIcon.relocate((width - stopText.getLayoutBounds().getWidth()) - width * 0.065, width * 0.022);

            stopTimeText.setFont(Fonts.robotoRegular(width * 0.12));
            stopTimeText.setX((width - stopTimeText.getLayoutBounds().getWidth()) - width * 0.05);
            stopTimeText.setY(width * 0.0825);

            hourText.setFont(Fonts.robotoRegular(width * 0.11));
            hourUnitText.setFont(Fonts.robotoRegular(width * 0.05));
            minuteText.setFont(Fonts.robotoRegular(width * 0.11));
            minuteUnitText.setFont(Fonts.robotoRegular(width * 0.05));

            if (Double.compare(durationBox.getLayoutBounds().getWidth(), 0) == 0) {
                durationBox.setLayoutX(width * 0.306);
                durationBox.setLayoutY(height * 0.54);
            } else {
                durationBox.relocate((width - durationBox.getLayoutBounds().getWidth()) * 0.5, height * 0.54);
            }
            durationBox.setSpacing(width * 0.0125);
            HBox.setMargin(hourUnitText, new Insets(0, width * 0.0125, 0, 0));
            HBox.setMargin(minuteText, new Insets(0, 0, 0, width * 0.0125));

            canvas.setWidth(width * 0.75);
            canvas.setHeight(width * 0.75);
            drawTickmarks();
            canvas.relocate((width - canvas.getWidth()) * 0.5, height * 0.30693069);

            barBackground.setStrokeWidth(width * 0.115);
            barBackground.setCenterX(width * 0.5);
            barBackground.setCenterY(height * 0.6039604);
            barBackground.setRadiusX(width * 0.4425);
            barBackground.setRadiusY(width * 0.4425);

            barBackground.setStrokeWidth(width * 0.115);
            barBackground.setCenterX(width * 0.5);
            barBackground.setCenterY(height * 0.6039604);
            barBackground.setRadiusX(width * 0.4425);
            barBackground.setRadiusY(width * 0.4425);

            barRotate.setPivotX(width * 0.5);
            barRotate.setPivotY(height * 0.6039604);

            bar.setStrokeWidth(width * 0.115);
            bar.setCenterX(width * 0.5);
            bar.setCenterY(height * 0.6039604);
            bar.setRadiusX(width * 0.4425);
            bar.setRadiusY(width * 0.4425);

            touchRotate.setPivotX(width * 0.5);
            touchRotate.setPivotY(height * 0.6039604);

            touchPointStart.setCenterX(bar.getCenterX() + bar.getRadiusX() * Math.sin(Math.toRadians(-getStartTime().toSecondOfDay() * ANGLE_STEP)));
            touchPointStart.setCenterY(bar.getCenterY() + bar.getRadiusY() * Math.cos(Math.toRadians(-getStartTime().toSecondOfDay() * ANGLE_STEP)));
            touchPointStart.setRadius(width * 0.0525);

            touchPointStop.setCenterX(bar.getCenterX() + bar.getRadiusX() * Math.sin(Math.toRadians(-getStopTime().toSecondOfDay() * ANGLE_STEP)));
            touchPointStop.setCenterY(bar.getCenterY() + bar.getRadiusY() * Math.cos(Math.toRadians(-getStopTime().toSecondOfDay() * ANGLE_STEP)));
            touchPointStop.setRadius(width * 0.0525);

            startPointIcon.setPrefSize(iconSize, iconSize);
            startPointIcon.relocate(touchPointStart.getBoundsInParent().getMinX() + iconSize * 0.6, touchPointStart.getBoundsInParent().getMinY() + iconSize * 0.6);

            stopPointIcon.setPrefSize(iconSize, iconSize);
            stopPointIcon.relocate(touchPointStop.getBoundsInParent().getMinX() + iconSize * 0.6, touchPointStop.getBoundsInParent().getMinY() + iconSize * 0.6);

            redraw();
        }
    }

    private void redraw() {
        startIcon.setBackground(new Background(new BackgroundFill(getBarColor(), CornerRadii.EMPTY, Insets.EMPTY)));
        stopIcon.setBackground(new Background(new BackgroundFill(getBarColor(), CornerRadii.EMPTY, Insets.EMPTY)));

        startText.setFill(getTextColor());
        startTimeText.setFill(getTextColor());

        stopText.setFill(getTextColor());
        stopTimeText.setFill(getTextColor());

        hourText.setFill(getTextColor());
        hourUnitText.setFill(getTextColor());
        minuteText.setFill(getTextColor());
        minuteUnitText.setFill(getTextColor());

        barBackground.setStroke(getBarBackgroundColor());
        bar.setStroke(getBarColor());

        touchPointStart.setFill(getBarBackgroundColor());
        touchPointStop.setFill(getBarBackgroundColor());

        startPointIcon.setBackground(new Background(new BackgroundFill(getBarColor(), CornerRadii.EMPTY, Insets.EMPTY)));
        stopPointIcon.setBackground(new Background(new BackgroundFill(getBarColor(), CornerRadii.EMPTY, Insets.EMPTY)));

        pane.setBackground(new Background(new BackgroundFill(backgroundPaint, CornerRadii.EMPTY, Insets.EMPTY)));
        pane.setBorder(new Border(new BorderStroke(borderPaint, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(borderWidth / PREFERRED_WIDTH * size))));
    }
}
