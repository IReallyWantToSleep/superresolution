/*
 * Super Resolution
 * Copyright (c) 2025. 187J3X1-114514
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.homo.superresolution.core.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.homo.superresolution.core.gui.core.backends.nanovg.NanoVGFont;
import io.homo.superresolution.core.utils.FileReadHelper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MaterialSymbols {
    private static final Map<String, MaterialSymbol> symbols = new HashMap<>();
    private static final NanoVGFont iconFont = new NanoVGFont("MaterialSymbolsRounded400", "/assets/super_resolution/font/MaterialSymbolsRounded400.ttf");

    public static void init() {
        iconFont.load();
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> map = gson.fromJson(String.join("\n", FileReadHelper.readText("/assets/super_resolution/font/MaterialSymbolsRounded400.codepoints")), type);
        map.forEach((name, codepoint) -> {
            char[] chars = Character.toChars(Integer.parseInt(codepoint.substring(2), 16));
            codepoint = new String(chars);
            symbols.put(
                    name,
                    new MaterialSymbol(
                            name,
                            codepoint,
                            iconFont
                    )
            );
        });
    }

    public static MaterialSymbol icon10k() {
        return symbols.get("10k");
    }

    public static MaterialSymbol icon10mp() {
        return symbols.get("10mp");
    }

    public static MaterialSymbol icon11mp() {
        return symbols.get("11mp");
    }

    public static MaterialSymbol icon123() {
        return symbols.get("123");
    }

    public static MaterialSymbol icon12mp() {
        return symbols.get("12mp");
    }

    public static MaterialSymbol icon13mp() {
        return symbols.get("13mp");
    }

    public static MaterialSymbol icon14mp() {
        return symbols.get("14mp");
    }

    public static MaterialSymbol icon15mp() {
        return symbols.get("15mp");
    }

    public static MaterialSymbol icon16mp() {
        return symbols.get("16mp");
    }

    public static MaterialSymbol icon17mp() {
        return symbols.get("17mp");
    }

    public static MaterialSymbol icon18UpRating() {
        return symbols.get("18_up_rating");
    }

    public static MaterialSymbol icon18mp() {
        return symbols.get("18mp");
    }

    public static MaterialSymbol icon19mp() {
        return symbols.get("19mp");
    }

    public static MaterialSymbol icon1k() {
        return symbols.get("1k");
    }

    public static MaterialSymbol icon1kPlus() {
        return symbols.get("1k_plus");
    }

    public static MaterialSymbol icon1xMobiledata() {
        return symbols.get("1x_mobiledata");
    }

    public static MaterialSymbol icon1xMobiledataBadge() {
        return symbols.get("1x_mobiledata_badge");
    }

    public static MaterialSymbol icon20mp() {
        return symbols.get("20mp");
    }

    public static MaterialSymbol icon21mp() {
        return symbols.get("21mp");
    }

    public static MaterialSymbol icon22mp() {
        return symbols.get("22mp");
    }

    public static MaterialSymbol icon23mp() {
        return symbols.get("23mp");
    }

    public static MaterialSymbol icon24fpsSelect() {
        return symbols.get("24fps_select");
    }

    public static MaterialSymbol icon24mp() {
        return symbols.get("24mp");
    }

    public static MaterialSymbol icon2d() {
        return symbols.get("2d");
    }

    public static MaterialSymbol icon2k() {
        return symbols.get("2k");
    }

    public static MaterialSymbol icon2kPlus() {
        return symbols.get("2k_plus");
    }

    public static MaterialSymbol icon2mp() {
        return symbols.get("2mp");
    }

    public static MaterialSymbol icon30fps() {
        return symbols.get("30fps");
    }

    public static MaterialSymbol icon30fpsSelect() {
        return symbols.get("30fps_select");
    }

    public static MaterialSymbol icon360() {
        return symbols.get("360");
    }

    public static MaterialSymbol icon3d() {
        return symbols.get("3d");
    }

    public static MaterialSymbol icon3dRotation() {
        return symbols.get("3d_rotation");
    }

    public static MaterialSymbol icon3gMobiledata() {
        return symbols.get("3g_mobiledata");
    }

    public static MaterialSymbol icon3gMobiledataBadge() {
        return symbols.get("3g_mobiledata_badge");
    }

    public static MaterialSymbol icon3k() {
        return symbols.get("3k");
    }

    public static MaterialSymbol icon3kPlus() {
        return symbols.get("3k_plus");
    }

    public static MaterialSymbol icon3mp() {
        return symbols.get("3mp");
    }

    public static MaterialSymbol icon3p() {
        return symbols.get("3p");
    }

    public static MaterialSymbol icon4gMobiledata() {
        return symbols.get("4g_mobiledata");
    }

    public static MaterialSymbol icon4gMobiledataBadge() {
        return symbols.get("4g_mobiledata_badge");
    }

    public static MaterialSymbol icon4gPlusMobiledata() {
        return symbols.get("4g_plus_mobiledata");
    }

    public static MaterialSymbol icon4k() {
        return symbols.get("4k");
    }

    public static MaterialSymbol icon4kPlus() {
        return symbols.get("4k_plus");
    }

    public static MaterialSymbol icon4mp() {
        return symbols.get("4mp");
    }

    public static MaterialSymbol icon50mp() {
        return symbols.get("50mp");
    }

    public static MaterialSymbol icon5g() {
        return symbols.get("5g");
    }

    public static MaterialSymbol icon5gMobiledataBadge() {
        return symbols.get("5g_mobiledata_badge");
    }

    public static MaterialSymbol icon5k() {
        return symbols.get("5k");
    }

    public static MaterialSymbol icon5kPlus() {
        return symbols.get("5k_plus");
    }

    public static MaterialSymbol icon5mp() {
        return symbols.get("5mp");
    }

    public static MaterialSymbol icon60fps() {
        return symbols.get("60fps");
    }

    public static MaterialSymbol icon60fpsSelect() {
        return symbols.get("60fps_select");
    }

    public static MaterialSymbol icon6FtApart() {
        return symbols.get("6_ft_apart");
    }

    public static MaterialSymbol icon6k() {
        return symbols.get("6k");
    }

    public static MaterialSymbol icon6kPlus() {
        return symbols.get("6k_plus");
    }

    public static MaterialSymbol icon6mp() {
        return symbols.get("6mp");
    }

    public static MaterialSymbol icon7k() {
        return symbols.get("7k");
    }

    public static MaterialSymbol icon7kPlus() {
        return symbols.get("7k_plus");
    }

    public static MaterialSymbol icon7mp() {
        return symbols.get("7mp");
    }

    public static MaterialSymbol icon8k() {
        return symbols.get("8k");
    }

    public static MaterialSymbol icon8kPlus() {
        return symbols.get("8k_plus");
    }

    public static MaterialSymbol icon8mp() {
        return symbols.get("8mp");
    }

    public static MaterialSymbol icon9k() {
        return symbols.get("9k");
    }

    public static MaterialSymbol icon9kPlus() {
        return symbols.get("9k_plus");
    }

    public static MaterialSymbol icon9mp() {
        return symbols.get("9mp");
    }

    public static MaterialSymbol iconAbc() {
        return symbols.get("abc");
    }

    public static MaterialSymbol iconAcUnit() {
        return symbols.get("ac_unit");
    }

    public static MaterialSymbol iconAccessAlarm() {
        return symbols.get("access_alarm");
    }

    public static MaterialSymbol iconAccessAlarms() {
        return symbols.get("access_alarms");
    }

    public static MaterialSymbol iconAccessTime() {
        return symbols.get("access_time");
    }

    public static MaterialSymbol iconAccessTimeFilled() {
        return symbols.get("access_time_filled");
    }

    public static MaterialSymbol iconAccessibility() {
        return symbols.get("accessibility");
    }

    public static MaterialSymbol iconAccessibilityNew() {
        return symbols.get("accessibility_new");
    }

    public static MaterialSymbol iconAccessible() {
        return symbols.get("accessible");
    }

    public static MaterialSymbol iconAccessibleForward() {
        return symbols.get("accessible_forward");
    }

    public static MaterialSymbol iconAccessibleMenu() {
        return symbols.get("accessible_menu");
    }

    public static MaterialSymbol iconAccountBalance() {
        return symbols.get("account_balance");
    }

    public static MaterialSymbol iconAccountBalanceWallet() {
        return symbols.get("account_balance_wallet");
    }

    public static MaterialSymbol iconAccountBox() {
        return symbols.get("account_box");
    }

    public static MaterialSymbol iconAccountChild() {
        return symbols.get("account_child");
    }

    public static MaterialSymbol iconAccountChildInvert() {
        return symbols.get("account_child_invert");
    }

    public static MaterialSymbol iconAccountCircle() {
        return symbols.get("account_circle");
    }

    public static MaterialSymbol iconAccountCircleFilled() {
        return symbols.get("account_circle_filled");
    }

    public static MaterialSymbol iconAccountCircleOff() {
        return symbols.get("account_circle_off");
    }

    public static MaterialSymbol iconAccountTree() {
        return symbols.get("account_tree");
    }

    public static MaterialSymbol iconActionKey() {
        return symbols.get("action_key");
    }

    public static MaterialSymbol iconActivityZone() {
        return symbols.get("activity_zone");
    }

    public static MaterialSymbol iconAcupuncture() {
        return symbols.get("acupuncture");
    }

    public static MaterialSymbol iconAcute() {
        return symbols.get("acute");
    }

    public static MaterialSymbol iconAd() {
        return symbols.get("ad");
    }

    public static MaterialSymbol iconAdGroup() {
        return symbols.get("ad_group");
    }

    public static MaterialSymbol iconAdGroupOff() {
        return symbols.get("ad_group_off");
    }

    public static MaterialSymbol iconAdOff() {
        return symbols.get("ad_off");
    }

    public static MaterialSymbol iconAdUnits() {
        return symbols.get("ad_units");
    }

    public static MaterialSymbol iconAdaptiveAudioMic() {
        return symbols.get("adaptive_audio_mic");
    }

    public static MaterialSymbol iconAdaptiveAudioMicOff() {
        return symbols.get("adaptive_audio_mic_off");
    }

    public static MaterialSymbol iconAdb() {
        return symbols.get("adb");
    }

    public static MaterialSymbol iconAdd() {
        return symbols.get("add");
    }

    public static MaterialSymbol iconAdd2() {
        return symbols.get("add_2");
    }

    public static MaterialSymbol iconAddAPhoto() {
        return symbols.get("add_a_photo");
    }

    public static MaterialSymbol iconAddAd() {
        return symbols.get("add_ad");
    }

    public static MaterialSymbol iconAddAlarm() {
        return symbols.get("add_alarm");
    }

    public static MaterialSymbol iconAddAlert() {
        return symbols.get("add_alert");
    }

    public static MaterialSymbol iconAddBox() {
        return symbols.get("add_box");
    }

    public static MaterialSymbol iconAddBusiness() {
        return symbols.get("add_business");
    }

    public static MaterialSymbol iconAddCall() {
        return symbols.get("add_call");
    }

    public static MaterialSymbol iconAddCard() {
        return symbols.get("add_card");
    }

    public static MaterialSymbol iconAddChart() {
        return symbols.get("add_chart");
    }

    public static MaterialSymbol iconAddCircle() {
        return symbols.get("add_circle");
    }

    public static MaterialSymbol iconAddCircleOutline() {
        return symbols.get("add_circle_outline");
    }

    public static MaterialSymbol iconAddColumnLeft() {
        return symbols.get("add_column_left");
    }

    public static MaterialSymbol iconAddColumnRight() {
        return symbols.get("add_column_right");
    }

    public static MaterialSymbol iconAddComment() {
        return symbols.get("add_comment");
    }

    public static MaterialSymbol iconAddDiamond() {
        return symbols.get("add_diamond");
    }

    public static MaterialSymbol iconAddHome() {
        return symbols.get("add_home");
    }

    public static MaterialSymbol iconAddHomeWork() {
        return symbols.get("add_home_work");
    }

    public static MaterialSymbol iconAddIcCall() {
        return symbols.get("add_ic_call");
    }

    public static MaterialSymbol iconAddLink() {
        return symbols.get("add_link");
    }

    public static MaterialSymbol iconAddLocation() {
        return symbols.get("add_location");
    }

    public static MaterialSymbol iconAddLocationAlt() {
        return symbols.get("add_location_alt");
    }

    public static MaterialSymbol iconAddModerator() {
        return symbols.get("add_moderator");
    }

    public static MaterialSymbol iconAddNotes() {
        return symbols.get("add_notes");
    }

    public static MaterialSymbol iconAddPhotoAlternate() {
        return symbols.get("add_photo_alternate");
    }

    public static MaterialSymbol iconAddReaction() {
        return symbols.get("add_reaction");
    }

    public static MaterialSymbol iconAddRoad() {
        return symbols.get("add_road");
    }

    public static MaterialSymbol iconAddRowAbove() {
        return symbols.get("add_row_above");
    }

    public static MaterialSymbol iconAddRowBelow() {
        return symbols.get("add_row_below");
    }

    public static MaterialSymbol iconAddShoppingCart() {
        return symbols.get("add_shopping_cart");
    }

    public static MaterialSymbol iconAddTask() {
        return symbols.get("add_task");
    }

    public static MaterialSymbol iconAddToDrive() {
        return symbols.get("add_to_drive");
    }

    public static MaterialSymbol iconAddToHomeScreen() {
        return symbols.get("add_to_home_screen");
    }

    public static MaterialSymbol iconAddToPhotos() {
        return symbols.get("add_to_photos");
    }

    public static MaterialSymbol iconAddToQueue() {
        return symbols.get("add_to_queue");
    }

    public static MaterialSymbol iconAddTriangle() {
        return symbols.get("add_triangle");
    }

    public static MaterialSymbol iconAddchart() {
        return symbols.get("addchart");
    }

    public static MaterialSymbol iconAdfScanner() {
        return symbols.get("adf_scanner");
    }

    public static MaterialSymbol iconAdjust() {
        return symbols.get("adjust");
    }

    public static MaterialSymbol iconAdminMeds() {
        return symbols.get("admin_meds");
    }

    public static MaterialSymbol iconAdminPanelSettings() {
        return symbols.get("admin_panel_settings");
    }

    public static MaterialSymbol iconAdsClick() {
        return symbols.get("ads_click");
    }

    public static MaterialSymbol iconAgender() {
        return symbols.get("agender");
    }

    public static MaterialSymbol iconAgriculture() {
        return symbols.get("agriculture");
    }

    public static MaterialSymbol iconAir() {
        return symbols.get("air");
    }

    public static MaterialSymbol iconAirFreshener() {
        return symbols.get("air_freshener");
    }

    public static MaterialSymbol iconAirPurifier() {
        return symbols.get("air_purifier");
    }

    public static MaterialSymbol iconAirPurifierGen() {
        return symbols.get("air_purifier_gen");
    }

    public static MaterialSymbol iconAirlineSeatFlat() {
        return symbols.get("airline_seat_flat");
    }

    public static MaterialSymbol iconAirlineSeatFlatAngled() {
        return symbols.get("airline_seat_flat_angled");
    }

    public static MaterialSymbol iconAirlineSeatIndividualSuite() {
        return symbols.get("airline_seat_individual_suite");
    }

    public static MaterialSymbol iconAirlineSeatLegroomExtra() {
        return symbols.get("airline_seat_legroom_extra");
    }

    public static MaterialSymbol iconAirlineSeatLegroomNormal() {
        return symbols.get("airline_seat_legroom_normal");
    }

    public static MaterialSymbol iconAirlineSeatLegroomReduced() {
        return symbols.get("airline_seat_legroom_reduced");
    }

    public static MaterialSymbol iconAirlineSeatReclineExtra() {
        return symbols.get("airline_seat_recline_extra");
    }

    public static MaterialSymbol iconAirlineSeatReclineNormal() {
        return symbols.get("airline_seat_recline_normal");
    }

    public static MaterialSymbol iconAirlineStops() {
        return symbols.get("airline_stops");
    }

    public static MaterialSymbol iconAirlines() {
        return symbols.get("airlines");
    }

    public static MaterialSymbol iconAirplaneTicket() {
        return symbols.get("airplane_ticket");
    }

    public static MaterialSymbol iconAirplanemodeActive() {
        return symbols.get("airplanemode_active");
    }

    public static MaterialSymbol iconAirplanemodeInactive() {
        return symbols.get("airplanemode_inactive");
    }

    public static MaterialSymbol iconAirplay() {
        return symbols.get("airplay");
    }

    public static MaterialSymbol iconAirportShuttle() {
        return symbols.get("airport_shuttle");
    }

    public static MaterialSymbol iconAirware() {
        return symbols.get("airware");
    }

    public static MaterialSymbol iconAirwave() {
        return symbols.get("airwave");
    }

    public static MaterialSymbol iconAlarm() {
        return symbols.get("alarm");
    }

    public static MaterialSymbol iconAlarmAdd() {
        return symbols.get("alarm_add");
    }

    public static MaterialSymbol iconAlarmOff() {
        return symbols.get("alarm_off");
    }

    public static MaterialSymbol iconAlarmOn() {
        return symbols.get("alarm_on");
    }

    public static MaterialSymbol iconAlarmPause() {
        return symbols.get("alarm_pause");
    }

    public static MaterialSymbol iconAlarmSmartWake() {
        return symbols.get("alarm_smart_wake");
    }

    public static MaterialSymbol iconAlbum() {
        return symbols.get("album");
    }

    public static MaterialSymbol iconAlignCenter() {
        return symbols.get("align_center");
    }

    public static MaterialSymbol iconAlignEnd() {
        return symbols.get("align_end");
    }

    public static MaterialSymbol iconAlignFlexCenter() {
        return symbols.get("align_flex_center");
    }

    public static MaterialSymbol iconAlignFlexEnd() {
        return symbols.get("align_flex_end");
    }

    public static MaterialSymbol iconAlignFlexStart() {
        return symbols.get("align_flex_start");
    }

    public static MaterialSymbol iconAlignHorizontalCenter() {
        return symbols.get("align_horizontal_center");
    }

    public static MaterialSymbol iconAlignHorizontalLeft() {
        return symbols.get("align_horizontal_left");
    }

    public static MaterialSymbol iconAlignHorizontalRight() {
        return symbols.get("align_horizontal_right");
    }

    public static MaterialSymbol iconAlignItemsStretch() {
        return symbols.get("align_items_stretch");
    }

    public static MaterialSymbol iconAlignJustifyCenter() {
        return symbols.get("align_justify_center");
    }

    public static MaterialSymbol iconAlignJustifyFlexEnd() {
        return symbols.get("align_justify_flex_end");
    }

    public static MaterialSymbol iconAlignJustifyFlexStart() {
        return symbols.get("align_justify_flex_start");
    }

    public static MaterialSymbol iconAlignJustifySpaceAround() {
        return symbols.get("align_justify_space_around");
    }

    public static MaterialSymbol iconAlignJustifySpaceBetween() {
        return symbols.get("align_justify_space_between");
    }

    public static MaterialSymbol iconAlignJustifySpaceEven() {
        return symbols.get("align_justify_space_even");
    }

    public static MaterialSymbol iconAlignJustifyStretch() {
        return symbols.get("align_justify_stretch");
    }

    public static MaterialSymbol iconAlignSelfStretch() {
        return symbols.get("align_self_stretch");
    }

    public static MaterialSymbol iconAlignSpaceAround() {
        return symbols.get("align_space_around");
    }

    public static MaterialSymbol iconAlignSpaceBetween() {
        return symbols.get("align_space_between");
    }

    public static MaterialSymbol iconAlignSpaceEven() {
        return symbols.get("align_space_even");
    }

    public static MaterialSymbol iconAlignStart() {
        return symbols.get("align_start");
    }

    public static MaterialSymbol iconAlignStretch() {
        return symbols.get("align_stretch");
    }

    public static MaterialSymbol iconAlignVerticalBottom() {
        return symbols.get("align_vertical_bottom");
    }

    public static MaterialSymbol iconAlignVerticalCenter() {
        return symbols.get("align_vertical_center");
    }

    public static MaterialSymbol iconAlignVerticalTop() {
        return symbols.get("align_vertical_top");
    }

    public static MaterialSymbol iconAllInbox() {
        return symbols.get("all_inbox");
    }

    public static MaterialSymbol iconAllInclusive() {
        return symbols.get("all_inclusive");
    }

    public static MaterialSymbol iconAllMatch() {
        return symbols.get("all_match");
    }

    public static MaterialSymbol iconAllOut() {
        return symbols.get("all_out");
    }

    public static MaterialSymbol iconAllergies() {
        return symbols.get("allergies");
    }

    public static MaterialSymbol iconAllergy() {
        return symbols.get("allergy");
    }

    public static MaterialSymbol iconAltRoute() {
        return symbols.get("alt_route");
    }

    public static MaterialSymbol iconAlternateEmail() {
        return symbols.get("alternate_email");
    }

    public static MaterialSymbol iconAltitude() {
        return symbols.get("altitude");
    }

    public static MaterialSymbol iconAmbientScreen() {
        return symbols.get("ambient_screen");
    }

    public static MaterialSymbol iconAmbulance() {
        return symbols.get("ambulance");
    }

    public static MaterialSymbol iconAmend() {
        return symbols.get("amend");
    }

    public static MaterialSymbol iconAmpStories() {
        return symbols.get("amp_stories");
    }

    public static MaterialSymbol iconAnalytics() {
        return symbols.get("analytics");
    }

    public static MaterialSymbol iconAnchor() {
        return symbols.get("anchor");
    }

    public static MaterialSymbol iconAndroid() {
        return symbols.get("android");
    }

    public static MaterialSymbol iconAndroidCell4Bar() {
        return symbols.get("android_cell_4_bar");
    }

    public static MaterialSymbol iconAndroidCell4BarAlert() {
        return symbols.get("android_cell_4_bar_alert");
    }

    public static MaterialSymbol iconAndroidCell4BarOff() {
        return symbols.get("android_cell_4_bar_off");
    }

    public static MaterialSymbol iconAndroidCell4BarPlus() {
        return symbols.get("android_cell_4_bar_plus");
    }

    public static MaterialSymbol iconAndroidCell5Bar() {
        return symbols.get("android_cell_5_bar");
    }

    public static MaterialSymbol iconAndroidCell5BarAlert() {
        return symbols.get("android_cell_5_bar_alert");
    }

    public static MaterialSymbol iconAndroidCell5BarOff() {
        return symbols.get("android_cell_5_bar_off");
    }

    public static MaterialSymbol iconAndroidCell5BarPlus() {
        return symbols.get("android_cell_5_bar_plus");
    }

    public static MaterialSymbol iconAndroidCellDual4Bar() {
        return symbols.get("android_cell_dual_4_bar");
    }

    public static MaterialSymbol iconAndroidCellDual4BarAlert() {
        return symbols.get("android_cell_dual_4_bar_alert");
    }

    public static MaterialSymbol iconAndroidCellDual4BarPlus() {
        return symbols.get("android_cell_dual_4_bar_plus");
    }

    public static MaterialSymbol iconAndroidCellDual5Bar() {
        return symbols.get("android_cell_dual_5_bar");
    }

    public static MaterialSymbol iconAndroidCellDual5BarAlert() {
        return symbols.get("android_cell_dual_5_bar_alert");
    }

    public static MaterialSymbol iconAndroidCellDual5BarPlus() {
        return symbols.get("android_cell_dual_5_bar_plus");
    }

    public static MaterialSymbol iconAndroidWifi3Bar() {
        return symbols.get("android_wifi_3_bar");
    }

    public static MaterialSymbol iconAndroidWifi3BarAlert() {
        return symbols.get("android_wifi_3_bar_alert");
    }

    public static MaterialSymbol iconAndroidWifi3BarLock() {
        return symbols.get("android_wifi_3_bar_lock");
    }

    public static MaterialSymbol iconAndroidWifi3BarOff() {
        return symbols.get("android_wifi_3_bar_off");
    }

    public static MaterialSymbol iconAndroidWifi3BarPlus() {
        return symbols.get("android_wifi_3_bar_plus");
    }

    public static MaterialSymbol iconAndroidWifi3BarQuestion() {
        return symbols.get("android_wifi_3_bar_question");
    }

    public static MaterialSymbol iconAndroidWifi4Bar() {
        return symbols.get("android_wifi_4_bar");
    }

    public static MaterialSymbol iconAndroidWifi4BarAlert() {
        return symbols.get("android_wifi_4_bar_alert");
    }

    public static MaterialSymbol iconAndroidWifi4BarLock() {
        return symbols.get("android_wifi_4_bar_lock");
    }

    public static MaterialSymbol iconAndroidWifi4BarOff() {
        return symbols.get("android_wifi_4_bar_off");
    }

    public static MaterialSymbol iconAndroidWifi4BarPlus() {
        return symbols.get("android_wifi_4_bar_plus");
    }

    public static MaterialSymbol iconAndroidWifi4BarQuestion() {
        return symbols.get("android_wifi_4_bar_question");
    }

    public static MaterialSymbol iconAnimatedImages() {
        return symbols.get("animated_images");
    }

    public static MaterialSymbol iconAnimation() {
        return symbols.get("animation");
    }

    public static MaterialSymbol iconAnnouncement() {
        return symbols.get("announcement");
    }

    public static MaterialSymbol iconAod() {
        return symbols.get("aod");
    }

    public static MaterialSymbol iconAodTablet() {
        return symbols.get("aod_tablet");
    }

    public static MaterialSymbol iconAodWatch() {
        return symbols.get("aod_watch");
    }

    public static MaterialSymbol iconApartment() {
        return symbols.get("apartment");
    }

    public static MaterialSymbol iconApi() {
        return symbols.get("io/homo/superresolution/api");
    }

    public static MaterialSymbol iconApkDocument() {
        return symbols.get("apk_document");
    }

    public static MaterialSymbol iconApkInstall() {
        return symbols.get("apk_install");
    }

    public static MaterialSymbol iconAppBadging() {
        return symbols.get("app_badging");
    }

    public static MaterialSymbol iconAppBlocking() {
        return symbols.get("app_blocking");
    }

    public static MaterialSymbol iconAppPromo() {
        return symbols.get("app_promo");
    }

    public static MaterialSymbol iconAppRegistration() {
        return symbols.get("app_registration");
    }

    public static MaterialSymbol iconAppSettingsAlt() {
        return symbols.get("app_settings_alt");
    }

    public static MaterialSymbol iconAppShortcut() {
        return symbols.get("app_shortcut");
    }

    public static MaterialSymbol iconApparel() {
        return symbols.get("apparel");
    }

    public static MaterialSymbol iconApproval() {
        return symbols.get("approval");
    }

    public static MaterialSymbol iconApprovalDelegation() {
        return symbols.get("approval_delegation");
    }

    public static MaterialSymbol iconApprovalDelegationOff() {
        return symbols.get("approval_delegation_off");
    }

    public static MaterialSymbol iconApps() {
        return symbols.get("apps");
    }

    public static MaterialSymbol iconAppsOutage() {
        return symbols.get("apps_outage");
    }

    public static MaterialSymbol iconAq() {
        return symbols.get("aq");
    }

    public static MaterialSymbol iconAqIndoor() {
        return symbols.get("aq_indoor");
    }

    public static MaterialSymbol iconArOnYou() {
        return symbols.get("ar_on_you");
    }

    public static MaterialSymbol iconArStickers() {
        return symbols.get("ar_stickers");
    }

    public static MaterialSymbol iconArchitecture() {
        return symbols.get("architecture");
    }

    public static MaterialSymbol iconArchive() {
        return symbols.get("archive");
    }

    public static MaterialSymbol iconAreaChart() {
        return symbols.get("area_chart");
    }

    public static MaterialSymbol iconArmingCountdown() {
        return symbols.get("arming_countdown");
    }

    public static MaterialSymbol iconArrowAndEdge() {
        return symbols.get("arrow_and_edge");
    }

    public static MaterialSymbol iconArrowBack() {
        return symbols.get("arrow_back");
    }

    public static MaterialSymbol iconArrowBack2() {
        return symbols.get("arrow_back_2");
    }

    public static MaterialSymbol iconArrowBackIos() {
        return symbols.get("arrow_back_ios");
    }

    public static MaterialSymbol iconArrowBackIosNew() {
        return symbols.get("arrow_back_ios_new");
    }

    public static MaterialSymbol iconArrowCircleDown() {
        return symbols.get("arrow_circle_down");
    }

    public static MaterialSymbol iconArrowCircleLeft() {
        return symbols.get("arrow_circle_left");
    }

    public static MaterialSymbol iconArrowCircleRight() {
        return symbols.get("arrow_circle_right");
    }

    public static MaterialSymbol iconArrowCircleUp() {
        return symbols.get("arrow_circle_up");
    }

    public static MaterialSymbol iconArrowCoolDown() {
        return symbols.get("arrow_cool_down");
    }

    public static MaterialSymbol iconArrowDownward() {
        return symbols.get("arrow_downward");
    }

    public static MaterialSymbol iconArrowDownwardAlt() {
        return symbols.get("arrow_downward_alt");
    }

    public static MaterialSymbol iconArrowDropDown() {
        return symbols.get("arrow_drop_down");
    }

    public static MaterialSymbol iconArrowDropDownCircle() {
        return symbols.get("arrow_drop_down_circle");
    }

    public static MaterialSymbol iconArrowDropUp() {
        return symbols.get("arrow_drop_up");
    }

    public static MaterialSymbol iconArrowForward() {
        return symbols.get("arrow_forward");
    }

    public static MaterialSymbol iconArrowForwardIos() {
        return symbols.get("arrow_forward_ios");
    }

    public static MaterialSymbol iconArrowInsert() {
        return symbols.get("arrow_insert");
    }

    public static MaterialSymbol iconArrowLeft() {
        return symbols.get("arrow_left");
    }

    public static MaterialSymbol iconArrowLeftAlt() {
        return symbols.get("arrow_left_alt");
    }

    public static MaterialSymbol iconArrowMenuClose() {
        return symbols.get("arrow_menu_close");
    }

    public static MaterialSymbol iconArrowMenuOpen() {
        return symbols.get("arrow_menu_open");
    }

    public static MaterialSymbol iconArrowOrEdge() {
        return symbols.get("arrow_or_edge");
    }

    public static MaterialSymbol iconArrowOutward() {
        return symbols.get("arrow_outward");
    }

    public static MaterialSymbol iconArrowRange() {
        return symbols.get("arrow_range");
    }

    public static MaterialSymbol iconArrowRight() {
        return symbols.get("arrow_right");
    }

    public static MaterialSymbol iconArrowRightAlt() {
        return symbols.get("arrow_right_alt");
    }

    public static MaterialSymbol iconArrowSelectorTool() {
        return symbols.get("arrow_selector_tool");
    }

    public static MaterialSymbol iconArrowShapeUp() {
        return symbols.get("arrow_shape_up");
    }

    public static MaterialSymbol iconArrowShapeUpStack() {
        return symbols.get("arrow_shape_up_stack");
    }

    public static MaterialSymbol iconArrowShapeUpStack2() {
        return symbols.get("arrow_shape_up_stack_2");
    }

    public static MaterialSymbol iconArrowSplit() {
        return symbols.get("arrow_split");
    }

    public static MaterialSymbol iconArrowTopLeft() {
        return symbols.get("arrow_top_left");
    }

    public static MaterialSymbol iconArrowTopRight() {
        return symbols.get("arrow_top_right");
    }

    public static MaterialSymbol iconArrowUploadProgress() {
        return symbols.get("arrow_upload_progress");
    }

    public static MaterialSymbol iconArrowUploadReady() {
        return symbols.get("arrow_upload_ready");
    }

    public static MaterialSymbol iconArrowUpward() {
        return symbols.get("arrow_upward");
    }

    public static MaterialSymbol iconArrowUpwardAlt() {
        return symbols.get("arrow_upward_alt");
    }

    public static MaterialSymbol iconArrowWarmUp() {
        return symbols.get("arrow_warm_up");
    }

    public static MaterialSymbol iconArrowsInput() {
        return symbols.get("arrows_input");
    }

    public static MaterialSymbol iconArrowsMoreDown() {
        return symbols.get("arrows_more_down");
    }

    public static MaterialSymbol iconArrowsMoreUp() {
        return symbols.get("arrows_more_up");
    }

    public static MaterialSymbol iconArrowsOutput() {
        return symbols.get("arrows_output");
    }

    public static MaterialSymbol iconArrowsOutward() {
        return symbols.get("arrows_outward");
    }

    public static MaterialSymbol iconArtTrack() {
        return symbols.get("art_track");
    }

    public static MaterialSymbol iconArticle() {
        return symbols.get("article");
    }

    public static MaterialSymbol iconArticlePerson() {
        return symbols.get("article_person");
    }

    public static MaterialSymbol iconArticleShortcut() {
        return symbols.get("article_shortcut");
    }

    public static MaterialSymbol iconArtist() {
        return symbols.get("artist");
    }

    public static MaterialSymbol iconAspectRatio() {
        return symbols.get("aspect_ratio");
    }

    public static MaterialSymbol iconAssessment() {
        return symbols.get("assessment");
    }

    public static MaterialSymbol iconAssignment() {
        return symbols.get("assignment");
    }

    public static MaterialSymbol iconAssignmentAdd() {
        return symbols.get("assignment_add");
    }

    public static MaterialSymbol iconAssignmentGlobe() {
        return symbols.get("assignment_globe");
    }

    public static MaterialSymbol iconAssignmentInd() {
        return symbols.get("assignment_ind");
    }

    public static MaterialSymbol iconAssignmentLate() {
        return symbols.get("assignment_late");
    }

    public static MaterialSymbol iconAssignmentReturn() {
        return symbols.get("assignment_return");
    }

    public static MaterialSymbol iconAssignmentReturned() {
        return symbols.get("assignment_returned");
    }

    public static MaterialSymbol iconAssignmentTurnedIn() {
        return symbols.get("assignment_turned_in");
    }

    public static MaterialSymbol iconAssistWalker() {
        return symbols.get("assist_walker");
    }

    public static MaterialSymbol iconAssistant() {
        return symbols.get("assistant");
    }

    public static MaterialSymbol iconAssistantDevice() {
        return symbols.get("assistant_device");
    }

    public static MaterialSymbol iconAssistantDirection() {
        return symbols.get("assistant_direction");
    }

    public static MaterialSymbol iconAssistantNavigation() {
        return symbols.get("assistant_navigation");
    }

    public static MaterialSymbol iconAssistantOnHub() {
        return symbols.get("assistant_on_hub");
    }

    public static MaterialSymbol iconAssistantPhoto() {
        return symbols.get("assistant_photo");
    }

    public static MaterialSymbol iconAssuredWorkload() {
        return symbols.get("assured_workload");
    }

    public static MaterialSymbol iconAsterisk() {
        return symbols.get("asterisk");
    }

    public static MaterialSymbol iconAstrophotographyAuto() {
        return symbols.get("astrophotography_auto");
    }

    public static MaterialSymbol iconAstrophotographyOff() {
        return symbols.get("astrophotography_off");
    }

    public static MaterialSymbol iconAtm() {
        return symbols.get("atm");
    }

    public static MaterialSymbol iconAtr() {
        return symbols.get("atr");
    }

    public static MaterialSymbol iconAttachEmail() {
        return symbols.get("attach_email");
    }

    public static MaterialSymbol iconAttachFile() {
        return symbols.get("attach_file");
    }

    public static MaterialSymbol iconAttachFileAdd() {
        return symbols.get("attach_file_add");
    }

    public static MaterialSymbol iconAttachFileOff() {
        return symbols.get("attach_file_off");
    }

    public static MaterialSymbol iconAttachMoney() {
        return symbols.get("attach_money");
    }

    public static MaterialSymbol iconAttachment() {
        return symbols.get("attachment");
    }

    public static MaterialSymbol iconAttractions() {
        return symbols.get("attractions");
    }

    public static MaterialSymbol iconAttribution() {
        return symbols.get("attribution");
    }

    public static MaterialSymbol iconAudioDescription() {
        return symbols.get("audio_description");
    }

    public static MaterialSymbol iconAudioFile() {
        return symbols.get("audio_file");
    }

    public static MaterialSymbol iconAudioVideoReceiver() {
        return symbols.get("audio_video_receiver");
    }

    public static MaterialSymbol iconAudiotrack() {
        return symbols.get("audiotrack");
    }

    public static MaterialSymbol iconAutoActivityZone() {
        return symbols.get("auto_activity_zone");
    }

    public static MaterialSymbol iconAutoAwesome() {
        return symbols.get("auto_awesome");
    }

    public static MaterialSymbol iconAutoAwesomeMosaic() {
        return symbols.get("auto_awesome_mosaic");
    }

    public static MaterialSymbol iconAutoAwesomeMotion() {
        return symbols.get("auto_awesome_motion");
    }

    public static MaterialSymbol iconAutoDelete() {
        return symbols.get("auto_delete");
    }

    public static MaterialSymbol iconAutoDetectVoice() {
        return symbols.get("auto_detect_voice");
    }

    public static MaterialSymbol iconAutoDrawSolid() {
        return symbols.get("auto_draw_solid");
    }

    public static MaterialSymbol iconAutoFix() {
        return symbols.get("auto_fix");
    }

    public static MaterialSymbol iconAutoFixHigh() {
        return symbols.get("auto_fix_high");
    }

    public static MaterialSymbol iconAutoFixNormal() {
        return symbols.get("auto_fix_normal");
    }

    public static MaterialSymbol iconAutoFixOff() {
        return symbols.get("auto_fix_off");
    }

    public static MaterialSymbol iconAutoGraph() {
        return symbols.get("auto_graph");
    }

    public static MaterialSymbol iconAutoLabel() {
        return symbols.get("auto_label");
    }

    public static MaterialSymbol iconAutoMeetingRoom() {
        return symbols.get("auto_meeting_room");
    }

    public static MaterialSymbol iconAutoMode() {
        return symbols.get("auto_mode");
    }

    public static MaterialSymbol iconAutoReadPause() {
        return symbols.get("auto_read_pause");
    }

    public static MaterialSymbol iconAutoReadPlay() {
        return symbols.get("auto_read_play");
    }

    public static MaterialSymbol iconAutoSchedule() {
        return symbols.get("auto_schedule");
    }

    public static MaterialSymbol iconAutoStories() {
        return symbols.get("auto_stories");
    }

    public static MaterialSymbol iconAutoStoriesOff() {
        return symbols.get("auto_stories_off");
    }

    public static MaterialSymbol iconAutoTimer() {
        return symbols.get("auto_timer");
    }

    public static MaterialSymbol iconAutoTowing() {
        return symbols.get("auto_towing");
    }

    public static MaterialSymbol iconAutoTransmission() {
        return symbols.get("auto_transmission");
    }

    public static MaterialSymbol iconAutoVideocam() {
        return symbols.get("auto_videocam");
    }

    public static MaterialSymbol iconAutofpsSelect() {
        return symbols.get("autofps_select");
    }

    public static MaterialSymbol iconAutomation() {
        return symbols.get("automation");
    }

    public static MaterialSymbol iconAutopause() {
        return symbols.get("autopause");
    }

    public static MaterialSymbol iconAutopay() {
        return symbols.get("autopay");
    }

    public static MaterialSymbol iconAutoplay() {
        return symbols.get("autoplay");
    }

    public static MaterialSymbol iconAutorenew() {
        return symbols.get("autorenew");
    }

    public static MaterialSymbol iconAutostop() {
        return symbols.get("autostop");
    }

    public static MaterialSymbol iconAv1() {
        return symbols.get("av1");
    }

    public static MaterialSymbol iconAvTimer() {
        return symbols.get("av_timer");
    }

    public static MaterialSymbol iconAvc() {
        return symbols.get("avc");
    }

    public static MaterialSymbol iconAvgPace() {
        return symbols.get("avg_pace");
    }

    public static MaterialSymbol iconAvgTime() {
        return symbols.get("avg_time");
    }

    public static MaterialSymbol iconAwardMeal() {
        return symbols.get("award_meal");
    }

    public static MaterialSymbol iconAwardStar() {
        return symbols.get("award_star");
    }

    public static MaterialSymbol iconAzm() {
        return symbols.get("azm");
    }

    public static MaterialSymbol iconBabyChangingStation() {
        return symbols.get("baby_changing_station");
    }

    public static MaterialSymbol iconBackHand() {
        return symbols.get("back_hand");
    }

    public static MaterialSymbol iconBackToTab() {
        return symbols.get("back_to_tab");
    }

    public static MaterialSymbol iconBackgroundDotLarge() {
        return symbols.get("background_dot_large");
    }

    public static MaterialSymbol iconBackgroundDotSmall() {
        return symbols.get("background_dot_small");
    }

    public static MaterialSymbol iconBackgroundGridSmall() {
        return symbols.get("background_grid_small");
    }

    public static MaterialSymbol iconBackgroundReplace() {
        return symbols.get("background_replace");
    }

    public static MaterialSymbol iconBacklightHigh() {
        return symbols.get("backlight_high");
    }

    public static MaterialSymbol iconBacklightHighOff() {
        return symbols.get("backlight_high_off");
    }

    public static MaterialSymbol iconBacklightLow() {
        return symbols.get("backlight_low");
    }

    public static MaterialSymbol iconBackpack() {
        return symbols.get("backpack");
    }

    public static MaterialSymbol iconBackspace() {
        return symbols.get("backspace");
    }

    public static MaterialSymbol iconBackup() {
        return symbols.get("backup");
    }

    public static MaterialSymbol iconBackupTable() {
        return symbols.get("backup_table");
    }

    public static MaterialSymbol iconBadge() {
        return symbols.get("badge");
    }

    public static MaterialSymbol iconBadgeCriticalBattery() {
        return symbols.get("badge_critical_battery");
    }

    public static MaterialSymbol iconBadminton() {
        return symbols.get("badminton");
    }

    public static MaterialSymbol iconBakeryDining() {
        return symbols.get("bakery_dining");
    }

    public static MaterialSymbol iconBalance() {
        return symbols.get("balance");
    }

    public static MaterialSymbol iconBalcony() {
        return symbols.get("balcony");
    }

    public static MaterialSymbol iconBallot() {
        return symbols.get("ballot");
    }

    public static MaterialSymbol iconBarChart() {
        return symbols.get("bar_chart");
    }

    public static MaterialSymbol iconBarChart4Bars() {
        return symbols.get("bar_chart_4_bars");
    }

    public static MaterialSymbol iconBarChartOff() {
        return symbols.get("bar_chart_off");
    }

    public static MaterialSymbol iconBarcode() {
        return symbols.get("barcode");
    }

    public static MaterialSymbol iconBarcodeReader() {
        return symbols.get("barcode_reader");
    }

    public static MaterialSymbol iconBarcodeScanner() {
        return symbols.get("barcode_scanner");
    }

    public static MaterialSymbol iconBarefoot() {
        return symbols.get("barefoot");
    }

    public static MaterialSymbol iconBatchPrediction() {
        return symbols.get("batch_prediction");
    }

    public static MaterialSymbol iconBathBedrock() {
        return symbols.get("bath_bedrock");
    }

    public static MaterialSymbol iconBathOutdoor() {
        return symbols.get("bath_outdoor");
    }

    public static MaterialSymbol iconBathPrivate() {
        return symbols.get("bath_private");
    }

    public static MaterialSymbol iconBathPublicLarge() {
        return symbols.get("bath_public_large");
    }

    public static MaterialSymbol iconBathSoak() {
        return symbols.get("bath_soak");
    }

    public static MaterialSymbol iconBathroom() {
        return symbols.get("bathroom");
    }

    public static MaterialSymbol iconBathtub() {
        return symbols.get("bathtub");
    }

    public static MaterialSymbol iconBattery0Bar() {
        return symbols.get("battery_0_bar");
    }

    public static MaterialSymbol iconBattery1Bar() {
        return symbols.get("battery_1_bar");
    }

    public static MaterialSymbol iconBattery20() {
        return symbols.get("battery_20");
    }

    public static MaterialSymbol iconBattery2Bar() {
        return symbols.get("battery_2_bar");
    }

    public static MaterialSymbol iconBattery30() {
        return symbols.get("battery_30");
    }

    public static MaterialSymbol iconBattery3Bar() {
        return symbols.get("battery_3_bar");
    }

    public static MaterialSymbol iconBattery4Bar() {
        return symbols.get("battery_4_bar");
    }

    public static MaterialSymbol iconBattery50() {
        return symbols.get("battery_50");
    }

    public static MaterialSymbol iconBattery5Bar() {
        return symbols.get("battery_5_bar");
    }

    public static MaterialSymbol iconBattery60() {
        return symbols.get("battery_60");
    }

    public static MaterialSymbol iconBattery6Bar() {
        return symbols.get("battery_6_bar");
    }

    public static MaterialSymbol iconBattery80() {
        return symbols.get("battery_80");
    }

    public static MaterialSymbol iconBattery90() {
        return symbols.get("battery_90");
    }

    public static MaterialSymbol iconBatteryAlert() {
        return symbols.get("battery_alert");
    }

    public static MaterialSymbol iconBatteryAndroid0() {
        return symbols.get("battery_android_0");
    }

    public static MaterialSymbol iconBatteryAndroid1() {
        return symbols.get("battery_android_1");
    }

    public static MaterialSymbol iconBatteryAndroid2() {
        return symbols.get("battery_android_2");
    }

    public static MaterialSymbol iconBatteryAndroid3() {
        return symbols.get("battery_android_3");
    }

    public static MaterialSymbol iconBatteryAndroid4() {
        return symbols.get("battery_android_4");
    }

    public static MaterialSymbol iconBatteryAndroid5() {
        return symbols.get("battery_android_5");
    }

    public static MaterialSymbol iconBatteryAndroid6() {
        return symbols.get("battery_android_6");
    }

    public static MaterialSymbol iconBatteryAndroidAlert() {
        return symbols.get("battery_android_alert");
    }

    public static MaterialSymbol iconBatteryAndroidBolt() {
        return symbols.get("battery_android_bolt");
    }

    public static MaterialSymbol iconBatteryAndroidFrame1() {
        return symbols.get("battery_android_frame_1");
    }

    public static MaterialSymbol iconBatteryAndroidFrame2() {
        return symbols.get("battery_android_frame_2");
    }

    public static MaterialSymbol iconBatteryAndroidFrame3() {
        return symbols.get("battery_android_frame_3");
    }

    public static MaterialSymbol iconBatteryAndroidFrame4() {
        return symbols.get("battery_android_frame_4");
    }

    public static MaterialSymbol iconBatteryAndroidFrame5() {
        return symbols.get("battery_android_frame_5");
    }

    public static MaterialSymbol iconBatteryAndroidFrame6() {
        return symbols.get("battery_android_frame_6");
    }

    public static MaterialSymbol iconBatteryAndroidFrameAlert() {
        return symbols.get("battery_android_frame_alert");
    }

    public static MaterialSymbol iconBatteryAndroidFrameBolt() {
        return symbols.get("battery_android_frame_bolt");
    }

    public static MaterialSymbol iconBatteryAndroidFrameFull() {
        return symbols.get("battery_android_frame_full");
    }

    public static MaterialSymbol iconBatteryAndroidFramePlus() {
        return symbols.get("battery_android_frame_plus");
    }

    public static MaterialSymbol iconBatteryAndroidFrameQuestion() {
        return symbols.get("battery_android_frame_question");
    }

    public static MaterialSymbol iconBatteryAndroidFrameShare() {
        return symbols.get("battery_android_frame_share");
    }

    public static MaterialSymbol iconBatteryAndroidFrameShield() {
        return symbols.get("battery_android_frame_shield");
    }

    public static MaterialSymbol iconBatteryAndroidFull() {
        return symbols.get("battery_android_full");
    }

    public static MaterialSymbol iconBatteryAndroidPlus() {
        return symbols.get("battery_android_plus");
    }

    public static MaterialSymbol iconBatteryAndroidQuestion() {
        return symbols.get("battery_android_question");
    }

    public static MaterialSymbol iconBatteryAndroidShare() {
        return symbols.get("battery_android_share");
    }

    public static MaterialSymbol iconBatteryAndroidShield() {
        return symbols.get("battery_android_shield");
    }

    public static MaterialSymbol iconBatteryChange() {
        return symbols.get("battery_change");
    }

    public static MaterialSymbol iconBatteryCharging20() {
        return symbols.get("battery_charging_20");
    }

    public static MaterialSymbol iconBatteryCharging30() {
        return symbols.get("battery_charging_30");
    }

    public static MaterialSymbol iconBatteryCharging50() {
        return symbols.get("battery_charging_50");
    }

    public static MaterialSymbol iconBatteryCharging60() {
        return symbols.get("battery_charging_60");
    }

    public static MaterialSymbol iconBatteryCharging80() {
        return symbols.get("battery_charging_80");
    }

    public static MaterialSymbol iconBatteryCharging90() {
        return symbols.get("battery_charging_90");
    }

    public static MaterialSymbol iconBatteryChargingFull() {
        return symbols.get("battery_charging_full");
    }

    public static MaterialSymbol iconBatteryError() {
        return symbols.get("battery_error");
    }

    public static MaterialSymbol iconBatteryFull() {
        return symbols.get("battery_full");
    }

    public static MaterialSymbol iconBatteryFullAlt() {
        return symbols.get("battery_full_alt");
    }

    public static MaterialSymbol iconBatteryHoriz000() {
        return symbols.get("battery_horiz_000");
    }

    public static MaterialSymbol iconBatteryHoriz050() {
        return symbols.get("battery_horiz_050");
    }

    public static MaterialSymbol iconBatteryHoriz075() {
        return symbols.get("battery_horiz_075");
    }

    public static MaterialSymbol iconBatteryLow() {
        return symbols.get("battery_low");
    }

    public static MaterialSymbol iconBatteryPlus() {
        return symbols.get("battery_plus");
    }

    public static MaterialSymbol iconBatteryProfile() {
        return symbols.get("battery_profile");
    }

    public static MaterialSymbol iconBatterySaver() {
        return symbols.get("battery_saver");
    }

    public static MaterialSymbol iconBatteryShare() {
        return symbols.get("battery_share");
    }

    public static MaterialSymbol iconBatteryStatusGood() {
        return symbols.get("battery_status_good");
    }

    public static MaterialSymbol iconBatteryStd() {
        return symbols.get("battery_std");
    }

    public static MaterialSymbol iconBatteryUnknown() {
        return symbols.get("battery_unknown");
    }

    public static MaterialSymbol iconBatteryVert005() {
        return symbols.get("battery_vert_005");
    }

    public static MaterialSymbol iconBatteryVert020() {
        return symbols.get("battery_vert_020");
    }

    public static MaterialSymbol iconBatteryVert050() {
        return symbols.get("battery_vert_050");
    }

    public static MaterialSymbol iconBatteryVeryLow() {
        return symbols.get("battery_very_low");
    }

    public static MaterialSymbol iconBeachAccess() {
        return symbols.get("beach_access");
    }

    public static MaterialSymbol iconBed() {
        return symbols.get("bed");
    }

    public static MaterialSymbol iconBedroomBaby() {
        return symbols.get("bedroom_baby");
    }

    public static MaterialSymbol iconBedroomChild() {
        return symbols.get("bedroom_child");
    }

    public static MaterialSymbol iconBedroomParent() {
        return symbols.get("bedroom_parent");
    }

    public static MaterialSymbol iconBedtime() {
        return symbols.get("bedtime");
    }

    public static MaterialSymbol iconBedtimeOff() {
        return symbols.get("bedtime_off");
    }

    public static MaterialSymbol iconBeenhere() {
        return symbols.get("beenhere");
    }

    public static MaterialSymbol iconBeerMeal() {
        return symbols.get("beer_meal");
    }

    public static MaterialSymbol iconBento() {
        return symbols.get("bento");
    }

    public static MaterialSymbol iconBia() {
        return symbols.get("bia");
    }

    public static MaterialSymbol iconBidLandscape() {
        return symbols.get("bid_landscape");
    }

    public static MaterialSymbol iconBidLandscapeDisabled() {
        return symbols.get("bid_landscape_disabled");
    }

    public static MaterialSymbol iconBigtopUpdates() {
        return symbols.get("bigtop_updates");
    }

    public static MaterialSymbol iconBikeDock() {
        return symbols.get("bike_dock");
    }

    public static MaterialSymbol iconBikeLane() {
        return symbols.get("bike_lane");
    }

    public static MaterialSymbol iconBikeScooter() {
        return symbols.get("bike_scooter");
    }

    public static MaterialSymbol iconBiotech() {
        return symbols.get("biotech");
    }

    public static MaterialSymbol iconBlanket() {
        return symbols.get("blanket");
    }

    public static MaterialSymbol iconBlender() {
        return symbols.get("blender");
    }

    public static MaterialSymbol iconBlind() {
        return symbols.get("blind");
    }

    public static MaterialSymbol iconBlinds() {
        return symbols.get("blinds");
    }

    public static MaterialSymbol iconBlindsClosed() {
        return symbols.get("blinds_closed");
    }

    public static MaterialSymbol iconBlock() {
        return symbols.get("block");
    }

    public static MaterialSymbol iconBloodPressure() {
        return symbols.get("blood_pressure");
    }

    public static MaterialSymbol iconBloodtype() {
        return symbols.get("bloodtype");
    }

    public static MaterialSymbol iconBluetooth() {
        return symbols.get("bluetooth");
    }

    public static MaterialSymbol iconBluetoothAudio() {
        return symbols.get("bluetooth_audio");
    }

    public static MaterialSymbol iconBluetoothConnected() {
        return symbols.get("bluetooth_connected");
    }

    public static MaterialSymbol iconBluetoothDisabled() {
        return symbols.get("bluetooth_disabled");
    }

    public static MaterialSymbol iconBluetoothDrive() {
        return symbols.get("bluetooth_drive");
    }

    public static MaterialSymbol iconBluetoothSearching() {
        return symbols.get("bluetooth_searching");
    }

    public static MaterialSymbol iconBlurCircular() {
        return symbols.get("blur_circular");
    }

    public static MaterialSymbol iconBlurLinear() {
        return symbols.get("blur_linear");
    }

    public static MaterialSymbol iconBlurMedium() {
        return symbols.get("blur_medium");
    }

    public static MaterialSymbol iconBlurOff() {
        return symbols.get("blur_off");
    }

    public static MaterialSymbol iconBlurOn() {
        return symbols.get("blur_on");
    }

    public static MaterialSymbol iconBlurShort() {
        return symbols.get("blur_short");
    }

    public static MaterialSymbol iconBoatBus() {
        return symbols.get("boat_bus");
    }

    public static MaterialSymbol iconBoatRailway() {
        return symbols.get("boat_railway");
    }

    public static MaterialSymbol iconBodyFat() {
        return symbols.get("body_fat");
    }

    public static MaterialSymbol iconBodySystem() {
        return symbols.get("body_system");
    }

    public static MaterialSymbol iconBolt() {
        return symbols.get("bolt");
    }

    public static MaterialSymbol iconBomb() {
        return symbols.get("bomb");
    }

    public static MaterialSymbol iconBook() {
        return symbols.get("book");
    }

    public static MaterialSymbol iconBook2() {
        return symbols.get("book_2");
    }

    public static MaterialSymbol iconBook3() {
        return symbols.get("book_3");
    }

    public static MaterialSymbol iconBook4() {
        return symbols.get("book_4");
    }

    public static MaterialSymbol iconBook5() {
        return symbols.get("book_5");
    }

    public static MaterialSymbol iconBook6() {
        return symbols.get("book_6");
    }

    public static MaterialSymbol iconBookOnline() {
        return symbols.get("book_online");
    }

    public static MaterialSymbol iconBookRibbon() {
        return symbols.get("book_ribbon");
    }

    public static MaterialSymbol iconBookmark() {
        return symbols.get("bookmark");
    }

    public static MaterialSymbol iconBookmarkAdd() {
        return symbols.get("bookmark_add");
    }

    public static MaterialSymbol iconBookmarkAdded() {
        return symbols.get("bookmark_added");
    }

    public static MaterialSymbol iconBookmarkBag() {
        return symbols.get("bookmark_bag");
    }

    public static MaterialSymbol iconBookmarkBorder() {
        return symbols.get("bookmark_border");
    }

    public static MaterialSymbol iconBookmarkCheck() {
        return symbols.get("bookmark_check");
    }

    public static MaterialSymbol iconBookmarkFlag() {
        return symbols.get("bookmark_flag");
    }

    public static MaterialSymbol iconBookmarkHeart() {
        return symbols.get("bookmark_heart");
    }

    public static MaterialSymbol iconBookmarkManager() {
        return symbols.get("bookmark_manager");
    }

    public static MaterialSymbol iconBookmarkRemove() {
        return symbols.get("bookmark_remove");
    }

    public static MaterialSymbol iconBookmarkStar() {
        return symbols.get("bookmark_star");
    }

    public static MaterialSymbol iconBookmarks() {
        return symbols.get("bookmarks");
    }

    public static MaterialSymbol iconBooksMoviesAndMusic() {
        return symbols.get("books_movies_and_music");
    }

    public static MaterialSymbol iconBorderAll() {
        return symbols.get("border_all");
    }

    public static MaterialSymbol iconBorderBottom() {
        return symbols.get("border_bottom");
    }

    public static MaterialSymbol iconBorderClear() {
        return symbols.get("border_clear");
    }

    public static MaterialSymbol iconBorderColor() {
        return symbols.get("border_color");
    }

    public static MaterialSymbol iconBorderHorizontal() {
        return symbols.get("border_horizontal");
    }

    public static MaterialSymbol iconBorderInner() {
        return symbols.get("border_inner");
    }

    public static MaterialSymbol iconBorderLeft() {
        return symbols.get("border_left");
    }

    public static MaterialSymbol iconBorderOuter() {
        return symbols.get("border_outer");
    }

    public static MaterialSymbol iconBorderRight() {
        return symbols.get("border_right");
    }

    public static MaterialSymbol iconBorderStyle() {
        return symbols.get("border_style");
    }

    public static MaterialSymbol iconBorderTop() {
        return symbols.get("border_top");
    }

    public static MaterialSymbol iconBorderVertical() {
        return symbols.get("border_vertical");
    }

    public static MaterialSymbol iconBorg() {
        return symbols.get("borg");
    }

    public static MaterialSymbol iconBottomAppBar() {
        return symbols.get("bottom_app_bar");
    }

    public static MaterialSymbol iconBottomDrawer() {
        return symbols.get("bottom_drawer");
    }

    public static MaterialSymbol iconBottomNavigation() {
        return symbols.get("bottom_navigation");
    }

    public static MaterialSymbol iconBottomPanelClose() {
        return symbols.get("bottom_panel_close");
    }

    public static MaterialSymbol iconBottomPanelOpen() {
        return symbols.get("bottom_panel_open");
    }

    public static MaterialSymbol iconBottomRightClick() {
        return symbols.get("bottom_right_click");
    }

    public static MaterialSymbol iconBottomSheets() {
        return symbols.get("bottom_sheets");
    }

    public static MaterialSymbol iconBox() {
        return symbols.get("box");
    }

    public static MaterialSymbol iconBoxAdd() {
        return symbols.get("box_add");
    }

    public static MaterialSymbol iconBoxEdit() {
        return symbols.get("box_edit");
    }

    public static MaterialSymbol iconBoy() {
        return symbols.get("boy");
    }

    public static MaterialSymbol iconBrandAwareness() {
        return symbols.get("brand_awareness");
    }

    public static MaterialSymbol iconBrandFamily() {
        return symbols.get("brand_family");
    }

    public static MaterialSymbol iconBrandingWatermark() {
        return symbols.get("branding_watermark");
    }

    public static MaterialSymbol iconBreakfastDining() {
        return symbols.get("breakfast_dining");
    }

    public static MaterialSymbol iconBreakingNews() {
        return symbols.get("breaking_news");
    }

    public static MaterialSymbol iconBreakingNewsAlt1() {
        return symbols.get("breaking_news_alt_1");
    }

    public static MaterialSymbol iconBreastfeeding() {
        return symbols.get("breastfeeding");
    }

    public static MaterialSymbol iconBrick() {
        return symbols.get("brick");
    }

    public static MaterialSymbol iconBriefcaseMeal() {
        return symbols.get("briefcase_meal");
    }

    public static MaterialSymbol iconBrightness1() {
        return symbols.get("brightness_1");
    }

    public static MaterialSymbol iconBrightness2() {
        return symbols.get("brightness_2");
    }

    public static MaterialSymbol iconBrightness3() {
        return symbols.get("brightness_3");
    }

    public static MaterialSymbol iconBrightness4() {
        return symbols.get("brightness_4");
    }

    public static MaterialSymbol iconBrightness5() {
        return symbols.get("brightness_5");
    }

    public static MaterialSymbol iconBrightness6() {
        return symbols.get("brightness_6");
    }

    public static MaterialSymbol iconBrightness7() {
        return symbols.get("brightness_7");
    }

    public static MaterialSymbol iconBrightnessAlert() {
        return symbols.get("brightness_alert");
    }

    public static MaterialSymbol iconBrightnessAuto() {
        return symbols.get("brightness_auto");
    }

    public static MaterialSymbol iconBrightnessEmpty() {
        return symbols.get("brightness_empty");
    }

    public static MaterialSymbol iconBrightnessHigh() {
        return symbols.get("brightness_high");
    }

    public static MaterialSymbol iconBrightnessLow() {
        return symbols.get("brightness_low");
    }

    public static MaterialSymbol iconBrightnessMedium() {
        return symbols.get("brightness_medium");
    }

    public static MaterialSymbol iconBringYourOwnIp() {
        return symbols.get("bring_your_own_ip");
    }

    public static MaterialSymbol iconBroadcastOnHome() {
        return symbols.get("broadcast_on_home");
    }

    public static MaterialSymbol iconBroadcastOnPersonal() {
        return symbols.get("broadcast_on_personal");
    }

    public static MaterialSymbol iconBrokenImage() {
        return symbols.get("broken_image");
    }

    public static MaterialSymbol iconBrowse() {
        return symbols.get("browse");
    }

    public static MaterialSymbol iconBrowseActivity() {
        return symbols.get("browse_activity");
    }

    public static MaterialSymbol iconBrowseGallery() {
        return symbols.get("browse_gallery");
    }

    public static MaterialSymbol iconBrowserNotSupported() {
        return symbols.get("browser_not_supported");
    }

    public static MaterialSymbol iconBrowserUpdated() {
        return symbols.get("browser_updated");
    }

    public static MaterialSymbol iconBrunchDining() {
        return symbols.get("brunch_dining");
    }

    public static MaterialSymbol iconBrush() {
        return symbols.get("brush");
    }

    public static MaterialSymbol iconBubble() {
        return symbols.get("bubble");
    }

    public static MaterialSymbol iconBubbleChart() {
        return symbols.get("bubble_chart");
    }

    public static MaterialSymbol iconBubbles() {
        return symbols.get("bubbles");
    }

    public static MaterialSymbol iconBucketCheck() {
        return symbols.get("bucket_check");
    }

    public static MaterialSymbol iconBugReport() {
        return symbols.get("bug_report");
    }

    public static MaterialSymbol iconBuild() {
        return symbols.get("build");
    }

    public static MaterialSymbol iconBuildCircle() {
        return symbols.get("build_circle");
    }

    public static MaterialSymbol iconBungalow() {
        return symbols.get("bungalow");
    }

    public static MaterialSymbol iconBurstMode() {
        return symbols.get("burst_mode");
    }

    public static MaterialSymbol iconBusAlert() {
        return symbols.get("bus_alert");
    }

    public static MaterialSymbol iconBusRailway() {
        return symbols.get("bus_railway");
    }

    public static MaterialSymbol iconBusiness() {
        return symbols.get("business");
    }

    public static MaterialSymbol iconBusinessCenter() {
        return symbols.get("business_center");
    }

    public static MaterialSymbol iconBusinessChip() {
        return symbols.get("business_chip");
    }

    public static MaterialSymbol iconBusinessMessages() {
        return symbols.get("business_messages");
    }

    public static MaterialSymbol iconButtonsAlt() {
        return symbols.get("buttons_alt");
    }

    public static MaterialSymbol iconCabin() {
        return symbols.get("cabin");
    }

    public static MaterialSymbol iconCable() {
        return symbols.get("cable");
    }

    public static MaterialSymbol iconCableCar() {
        return symbols.get("cable_car");
    }

    public static MaterialSymbol iconCached() {
        return symbols.get("cached");
    }

    public static MaterialSymbol iconCadence() {
        return symbols.get("cadence");
    }

    public static MaterialSymbol iconCake() {
        return symbols.get("cake");
    }

    public static MaterialSymbol iconCakeAdd() {
        return symbols.get("cake_add");
    }

    public static MaterialSymbol iconCalculate() {
        return symbols.get("calculate");
    }

    public static MaterialSymbol iconCalendarAddOn() {
        return symbols.get("calendar_add_on");
    }

    public static MaterialSymbol iconCalendarAppsScript() {
        return symbols.get("calendar_apps_script");
    }

    public static MaterialSymbol iconCalendarCheck() {
        return symbols.get("calendar_check");
    }

    public static MaterialSymbol iconCalendarClock() {
        return symbols.get("calendar_clock");
    }

    public static MaterialSymbol iconCalendarLock() {
        return symbols.get("calendar_lock");
    }

    public static MaterialSymbol iconCalendarMeal() {
        return symbols.get("calendar_meal");
    }

    public static MaterialSymbol iconCalendarMeal2() {
        return symbols.get("calendar_meal_2");
    }

    public static MaterialSymbol iconCalendarMonth() {
        return symbols.get("calendar_month");
    }

    public static MaterialSymbol iconCalendarToday() {
        return symbols.get("calendar_today");
    }

    public static MaterialSymbol iconCalendarViewDay() {
        return symbols.get("calendar_view_day");
    }

    public static MaterialSymbol iconCalendarViewMonth() {
        return symbols.get("calendar_view_month");
    }

    public static MaterialSymbol iconCalendarViewWeek() {
        return symbols.get("calendar_view_week");
    }

    public static MaterialSymbol iconCall() {
        return symbols.get("call");
    }

    public static MaterialSymbol iconCallEnd() {
        return symbols.get("call_end");
    }

    public static MaterialSymbol iconCallEndAlt() {
        return symbols.get("call_end_alt");
    }

    public static MaterialSymbol iconCallLog() {
        return symbols.get("call_log");
    }

    public static MaterialSymbol iconCallMade() {
        return symbols.get("call_made");
    }

    public static MaterialSymbol iconCallMerge() {
        return symbols.get("call_merge");
    }

    public static MaterialSymbol iconCallMissed() {
        return symbols.get("call_missed");
    }

    public static MaterialSymbol iconCallMissedOutgoing() {
        return symbols.get("call_missed_outgoing");
    }

    public static MaterialSymbol iconCallQuality() {
        return symbols.get("call_quality");
    }

    public static MaterialSymbol iconCallReceived() {
        return symbols.get("call_received");
    }

    public static MaterialSymbol iconCallSplit() {
        return symbols.get("call_split");
    }

    public static MaterialSymbol iconCallToAction() {
        return symbols.get("call_to_action");
    }

    public static MaterialSymbol iconCamera() {
        return symbols.get("camera");
    }

    public static MaterialSymbol iconCameraAlt() {
        return symbols.get("camera_alt");
    }

    public static MaterialSymbol iconCameraEnhance() {
        return symbols.get("camera_enhance");
    }

    public static MaterialSymbol iconCameraFront() {
        return symbols.get("camera_front");
    }

    public static MaterialSymbol iconCameraIndoor() {
        return symbols.get("camera_indoor");
    }

    public static MaterialSymbol iconCameraOutdoor() {
        return symbols.get("camera_outdoor");
    }

    public static MaterialSymbol iconCameraRear() {
        return symbols.get("camera_rear");
    }

    public static MaterialSymbol iconCameraRoll() {
        return symbols.get("camera_roll");
    }

    public static MaterialSymbol iconCameraVideo() {
        return symbols.get("camera_video");
    }

    public static MaterialSymbol iconCameraswitch() {
        return symbols.get("cameraswitch");
    }

    public static MaterialSymbol iconCampaign() {
        return symbols.get("campaign");
    }

    public static MaterialSymbol iconCamping() {
        return symbols.get("camping");
    }

    public static MaterialSymbol iconCancel() {
        return symbols.get("cancel");
    }

    public static MaterialSymbol iconCancelPresentation() {
        return symbols.get("cancel_presentation");
    }

    public static MaterialSymbol iconCancelScheduleSend() {
        return symbols.get("cancel_schedule_send");
    }

    public static MaterialSymbol iconCandle() {
        return symbols.get("candle");
    }

    public static MaterialSymbol iconCandlestickChart() {
        return symbols.get("candlestick_chart");
    }

    public static MaterialSymbol iconCannabis() {
        return symbols.get("cannabis");
    }

    public static MaterialSymbol iconCaptivePortal() {
        return symbols.get("captive_portal");
    }

    public static MaterialSymbol iconCapture() {
        return symbols.get("capture");
    }

    public static MaterialSymbol iconCarCrash() {
        return symbols.get("car_crash");
    }

    public static MaterialSymbol iconCarDefrostLeft() {
        return symbols.get("car_defrost_left");
    }

    public static MaterialSymbol iconCarDefrostLowLeft() {
        return symbols.get("car_defrost_low_left");
    }

    public static MaterialSymbol iconCarDefrostLowRight() {
        return symbols.get("car_defrost_low_right");
    }

    public static MaterialSymbol iconCarDefrostMidLeft() {
        return symbols.get("car_defrost_mid_left");
    }

    public static MaterialSymbol iconCarDefrostMidLowLeft() {
        return symbols.get("car_defrost_mid_low_left");
    }

    public static MaterialSymbol iconCarDefrostMidLowRight() {
        return symbols.get("car_defrost_mid_low_right");
    }

    public static MaterialSymbol iconCarDefrostMidRight() {
        return symbols.get("car_defrost_mid_right");
    }

    public static MaterialSymbol iconCarDefrostRight() {
        return symbols.get("car_defrost_right");
    }

    public static MaterialSymbol iconCarFanLowLeft() {
        return symbols.get("car_fan_low_left");
    }

    public static MaterialSymbol iconCarFanLowMidLeft() {
        return symbols.get("car_fan_low_mid_left");
    }

    public static MaterialSymbol iconCarFanLowRight() {
        return symbols.get("car_fan_low_right");
    }

    public static MaterialSymbol iconCarFanMidLeft() {
        return symbols.get("car_fan_mid_left");
    }

    public static MaterialSymbol iconCarFanMidLowRight() {
        return symbols.get("car_fan_mid_low_right");
    }

    public static MaterialSymbol iconCarFanMidRight() {
        return symbols.get("car_fan_mid_right");
    }

    public static MaterialSymbol iconCarFanRecirculate() {
        return symbols.get("car_fan_recirculate");
    }

    public static MaterialSymbol iconCarGear() {
        return symbols.get("car_gear");
    }

    public static MaterialSymbol iconCarLock() {
        return symbols.get("car_lock");
    }

    public static MaterialSymbol iconCarMirrorHeat() {
        return symbols.get("car_mirror_heat");
    }

    public static MaterialSymbol iconCarRental() {
        return symbols.get("car_rental");
    }

    public static MaterialSymbol iconCarRepair() {
        return symbols.get("car_repair");
    }

    public static MaterialSymbol iconCarTag() {
        return symbols.get("car_tag");
    }

    public static MaterialSymbol iconCardGiftcard() {
        return symbols.get("card_giftcard");
    }

    public static MaterialSymbol iconCardMembership() {
        return symbols.get("card_membership");
    }

    public static MaterialSymbol iconCardTravel() {
        return symbols.get("card_travel");
    }

    public static MaterialSymbol iconCardioLoad() {
        return symbols.get("cardio_load");
    }

    public static MaterialSymbol iconCardiology() {
        return symbols.get("cardiology");
    }

    public static MaterialSymbol iconCards() {
        return symbols.get("cards");
    }

    public static MaterialSymbol iconCardsStar() {
        return symbols.get("cards_star");
    }

    public static MaterialSymbol iconCarpenter() {
        return symbols.get("carpenter");
    }

    public static MaterialSymbol iconCarryOnBag() {
        return symbols.get("carry_on_bag");
    }

    public static MaterialSymbol iconCarryOnBagChecked() {
        return symbols.get("carry_on_bag_checked");
    }

    public static MaterialSymbol iconCarryOnBagInactive() {
        return symbols.get("carry_on_bag_inactive");
    }

    public static MaterialSymbol iconCarryOnBagQuestion() {
        return symbols.get("carry_on_bag_question");
    }

    public static MaterialSymbol iconCases() {
        return symbols.get("cases");
    }

    public static MaterialSymbol iconCasino() {
        return symbols.get("casino");
    }

    public static MaterialSymbol iconCast() {
        return symbols.get("cast");
    }

    public static MaterialSymbol iconCastConnected() {
        return symbols.get("cast_connected");
    }

    public static MaterialSymbol iconCastForEducation() {
        return symbols.get("cast_for_education");
    }

    public static MaterialSymbol iconCastPause() {
        return symbols.get("cast_pause");
    }

    public static MaterialSymbol iconCastWarning() {
        return symbols.get("cast_warning");
    }

    public static MaterialSymbol iconCastle() {
        return symbols.get("castle");
    }

    public static MaterialSymbol iconCategory() {
        return symbols.get("category");
    }

    public static MaterialSymbol iconCategorySearch() {
        return symbols.get("category_search");
    }

    public static MaterialSymbol iconCelebration() {
        return symbols.get("celebration");
    }

    public static MaterialSymbol iconCellMerge() {
        return symbols.get("cell_merge");
    }

    public static MaterialSymbol iconCellTower() {
        return symbols.get("cell_tower");
    }

    public static MaterialSymbol iconCellWifi() {
        return symbols.get("cell_wifi");
    }

    public static MaterialSymbol iconCenterFocusStrong() {
        return symbols.get("center_focus_strong");
    }

    public static MaterialSymbol iconCenterFocusWeak() {
        return symbols.get("center_focus_weak");
    }

    public static MaterialSymbol iconChair() {
        return symbols.get("chair");
    }

    public static MaterialSymbol iconChairAlt() {
        return symbols.get("chair_alt");
    }

    public static MaterialSymbol iconChairCounter() {
        return symbols.get("chair_counter");
    }

    public static MaterialSymbol iconChairFireplace() {
        return symbols.get("chair_fireplace");
    }

    public static MaterialSymbol iconChairUmbrella() {
        return symbols.get("chair_umbrella");
    }

    public static MaterialSymbol iconChalet() {
        return symbols.get("chalet");
    }

    public static MaterialSymbol iconChangeCircle() {
        return symbols.get("change_circle");
    }

    public static MaterialSymbol iconChangeHistory() {
        return symbols.get("change_history");
    }

    public static MaterialSymbol iconCharger() {
        return symbols.get("charger");
    }

    public static MaterialSymbol iconChargingStation() {
        return symbols.get("charging_station");
    }

    public static MaterialSymbol iconChartData() {
        return symbols.get("chart_data");
    }

    public static MaterialSymbol iconChat() {
        return symbols.get("chat");
    }

    public static MaterialSymbol iconChatAddOn() {
        return symbols.get("chat_add_on");
    }

    public static MaterialSymbol iconChatAppsScript() {
        return symbols.get("chat_apps_script");
    }

    public static MaterialSymbol iconChatBubble() {
        return symbols.get("chat_bubble");
    }

    public static MaterialSymbol iconChatBubbleOutline() {
        return symbols.get("chat_bubble_outline");
    }

    public static MaterialSymbol iconChatDashed() {
        return symbols.get("chat_dashed");
    }

    public static MaterialSymbol iconChatError() {
        return symbols.get("chat_error");
    }

    public static MaterialSymbol iconChatInfo() {
        return symbols.get("chat_info");
    }

    public static MaterialSymbol iconChatPasteGo() {
        return symbols.get("chat_paste_go");
    }

    public static MaterialSymbol iconChatPasteGo2() {
        return symbols.get("chat_paste_go_2");
    }

    public static MaterialSymbol iconCheck() {
        return symbols.get("check");
    }

    public static MaterialSymbol iconCheckBox() {
        return symbols.get("check_box");
    }

    public static MaterialSymbol iconCheckBoxOutlineBlank() {
        return symbols.get("check_box_outline_blank");
    }

    public static MaterialSymbol iconCheckCircle() {
        return symbols.get("check_circle");
    }

    public static MaterialSymbol iconCheckCircleFilled() {
        return symbols.get("check_circle_filled");
    }

    public static MaterialSymbol iconCheckCircleOutline() {
        return symbols.get("check_circle_outline");
    }

    public static MaterialSymbol iconCheckCircleUnread() {
        return symbols.get("check_circle_unread");
    }

    public static MaterialSymbol iconCheckInOut() {
        return symbols.get("check_in_out");
    }

    public static MaterialSymbol iconCheckIndeterminateSmall() {
        return symbols.get("check_indeterminate_small");
    }

    public static MaterialSymbol iconCheckSmall() {
        return symbols.get("check_small");
    }

    public static MaterialSymbol iconCheckbook() {
        return symbols.get("checkbook");
    }

    public static MaterialSymbol iconCheckedBag() {
        return symbols.get("checked_bag");
    }

    public static MaterialSymbol iconCheckedBagQuestion() {
        return symbols.get("checked_bag_question");
    }

    public static MaterialSymbol iconChecklist() {
        return symbols.get("checklist");
    }

    public static MaterialSymbol iconChecklistRtl() {
        return symbols.get("checklist_rtl");
    }

    public static MaterialSymbol iconCheckroom() {
        return symbols.get("checkroom");
    }

    public static MaterialSymbol iconCheer() {
        return symbols.get("cheer");
    }

    public static MaterialSymbol iconChefHat() {
        return symbols.get("chef_hat");
    }

    public static MaterialSymbol iconChess() {
        return symbols.get("chess");
    }

    public static MaterialSymbol iconChessBishop() {
        return symbols.get("chess_bishop");
    }

    public static MaterialSymbol iconChessBishop2() {
        return symbols.get("chess_bishop_2");
    }

    public static MaterialSymbol iconChessKing() {
        return symbols.get("chess_king");
    }

    public static MaterialSymbol iconChessKing2() {
        return symbols.get("chess_king_2");
    }

    public static MaterialSymbol iconChessKnight() {
        return symbols.get("chess_knight");
    }

    public static MaterialSymbol iconChessPawn() {
        return symbols.get("chess_pawn");
    }

    public static MaterialSymbol iconChessPawn2() {
        return symbols.get("chess_pawn_2");
    }

    public static MaterialSymbol iconChessQueen() {
        return symbols.get("chess_queen");
    }

    public static MaterialSymbol iconChessRook() {
        return symbols.get("chess_rook");
    }

    public static MaterialSymbol iconChevronBackward() {
        return symbols.get("chevron_backward");
    }

    public static MaterialSymbol iconChevronForward() {
        return symbols.get("chevron_forward");
    }

    public static MaterialSymbol iconChevronLeft() {
        return symbols.get("chevron_left");
    }

    public static MaterialSymbol iconChevronRight() {
        return symbols.get("chevron_right");
    }

    public static MaterialSymbol iconChildCare() {
        return symbols.get("child_care");
    }

    public static MaterialSymbol iconChildFriendly() {
        return symbols.get("child_friendly");
    }

    public static MaterialSymbol iconChildHat() {
        return symbols.get("child_hat");
    }

    public static MaterialSymbol iconChipExtraction() {
        return symbols.get("chip_extraction");
    }

    public static MaterialSymbol iconChips() {
        return symbols.get("chips");
    }

    public static MaterialSymbol iconChromeReaderMode() {
        return symbols.get("chrome_reader_mode");
    }

    public static MaterialSymbol iconChromecast2() {
        return symbols.get("chromecast_2");
    }

    public static MaterialSymbol iconChromecastDevice() {
        return symbols.get("chromecast_device");
    }

    public static MaterialSymbol iconChronic() {
        return symbols.get("chronic");
    }

    public static MaterialSymbol iconChurch() {
        return symbols.get("church");
    }

    public static MaterialSymbol iconCinematicBlur() {
        return symbols.get("cinematic_blur");
    }

    public static MaterialSymbol iconCircle() {
        return symbols.get("circle");
    }

    public static MaterialSymbol iconCircleNotifications() {
        return symbols.get("circle_notifications");
    }

    public static MaterialSymbol iconCircles() {
        return symbols.get("circles");
    }

    public static MaterialSymbol iconCirclesExt() {
        return symbols.get("circles_ext");
    }

    public static MaterialSymbol iconClarify() {
        return symbols.get("clarify");
    }

    public static MaterialSymbol iconClass() {
        return symbols.get("class");
    }

    public static MaterialSymbol iconCleanHands() {
        return symbols.get("clean_hands");
    }

    public static MaterialSymbol iconCleaning() {
        return symbols.get("cleaning");
    }

    public static MaterialSymbol iconCleaningBucket() {
        return symbols.get("cleaning_bucket");
    }

    public static MaterialSymbol iconCleaningServices() {
        return symbols.get("cleaning_services");
    }

    public static MaterialSymbol iconClear() {
        return symbols.get("clear");
    }

    public static MaterialSymbol iconClearAll() {
        return symbols.get("clear_all");
    }

    public static MaterialSymbol iconClearDay() {
        return symbols.get("clear_day");
    }

    public static MaterialSymbol iconClearNight() {
        return symbols.get("clear_night");
    }

    public static MaterialSymbol iconClimateMiniSplit() {
        return symbols.get("climate_mini_split");
    }

    public static MaterialSymbol iconClinicalNotes() {
        return symbols.get("clinical_notes");
    }

    public static MaterialSymbol iconClockArrowDown() {
        return symbols.get("clock_arrow_down");
    }

    public static MaterialSymbol iconClockArrowUp() {
        return symbols.get("clock_arrow_up");
    }

    public static MaterialSymbol iconClockLoader10() {
        return symbols.get("clock_loader_10");
    }

    public static MaterialSymbol iconClockLoader20() {
        return symbols.get("clock_loader_20");
    }

    public static MaterialSymbol iconClockLoader40() {
        return symbols.get("clock_loader_40");
    }

    public static MaterialSymbol iconClockLoader60() {
        return symbols.get("clock_loader_60");
    }

    public static MaterialSymbol iconClockLoader80() {
        return symbols.get("clock_loader_80");
    }

    public static MaterialSymbol iconClockLoader90() {
        return symbols.get("clock_loader_90");
    }

    public static MaterialSymbol iconClose() {
        return symbols.get("close");
    }

    public static MaterialSymbol iconCloseFullscreen() {
        return symbols.get("close_fullscreen");
    }

    public static MaterialSymbol iconCloseSmall() {
        return symbols.get("close_small");
    }

    public static MaterialSymbol iconClosedCaption() {
        return symbols.get("closed_caption");
    }

    public static MaterialSymbol iconClosedCaptionAdd() {
        return symbols.get("closed_caption_add");
    }

    public static MaterialSymbol iconClosedCaptionDisabled() {
        return symbols.get("closed_caption_disabled");
    }

    public static MaterialSymbol iconClosedCaptionOff() {
        return symbols.get("closed_caption_off");
    }

    public static MaterialSymbol iconCloud() {
        return symbols.get("cloud");
    }

    public static MaterialSymbol iconCloudAlert() {
        return symbols.get("cloud_alert");
    }

    public static MaterialSymbol iconCloudCircle() {
        return symbols.get("cloud_circle");
    }

    public static MaterialSymbol iconCloudDone() {
        return symbols.get("cloud_done");
    }

    public static MaterialSymbol iconCloudDownload() {
        return symbols.get("cloud_download");
    }

    public static MaterialSymbol iconCloudLock() {
        return symbols.get("cloud_lock");
    }

    public static MaterialSymbol iconCloudOff() {
        return symbols.get("cloud_off");
    }

    public static MaterialSymbol iconCloudQueue() {
        return symbols.get("cloud_queue");
    }

    public static MaterialSymbol iconCloudSync() {
        return symbols.get("cloud_sync");
    }

    public static MaterialSymbol iconCloudUpload() {
        return symbols.get("cloud_upload");
    }

    public static MaterialSymbol iconCloudy() {
        return symbols.get("cloudy");
    }

    public static MaterialSymbol iconCloudyFilled() {
        return symbols.get("cloudy_filled");
    }

    public static MaterialSymbol iconCloudySnowing() {
        return symbols.get("cloudy_snowing");
    }

    public static MaterialSymbol iconCo2() {
        return symbols.get("co2");
    }

    public static MaterialSymbol iconCoPresent() {
        return symbols.get("co_present");
    }

    public static MaterialSymbol iconCode() {
        return symbols.get("code");
    }

    public static MaterialSymbol iconCodeBlocks() {
        return symbols.get("code_blocks");
    }

    public static MaterialSymbol iconCodeOff() {
        return symbols.get("code_off");
    }

    public static MaterialSymbol iconCoffee() {
        return symbols.get("coffee");
    }

    public static MaterialSymbol iconCoffeeMaker() {
        return symbols.get("coffee_maker");
    }

    public static MaterialSymbol iconCognition() {
        return symbols.get("cognition");
    }

    public static MaterialSymbol iconCognition2() {
        return symbols.get("cognition_2");
    }

    public static MaterialSymbol iconCollapseAll() {
        return symbols.get("collapse_all");
    }

    public static MaterialSymbol iconCollapseContent() {
        return symbols.get("collapse_content");
    }

    public static MaterialSymbol iconCollections() {
        return symbols.get("collections");
    }

    public static MaterialSymbol iconCollectionsBookmark() {
        return symbols.get("collections_bookmark");
    }

    public static MaterialSymbol iconColorLens() {
        return symbols.get("color_lens");
    }

    public static MaterialSymbol iconColorize() {
        return symbols.get("colorize");
    }

    public static MaterialSymbol iconColors() {
        return symbols.get("colors");
    }

    public static MaterialSymbol iconCombineColumns() {
        return symbols.get("combine_columns");
    }

    public static MaterialSymbol iconComedyMask() {
        return symbols.get("comedy_mask");
    }

    public static MaterialSymbol iconComicBubble() {
        return symbols.get("comic_bubble");
    }

    public static MaterialSymbol iconComment() {
        return symbols.get("comment");
    }

    public static MaterialSymbol iconCommentBank() {
        return symbols.get("comment_bank");
    }

    public static MaterialSymbol iconCommentsDisabled() {
        return symbols.get("comments_disabled");
    }

    public static MaterialSymbol iconCommit() {
        return symbols.get("commit");
    }

    public static MaterialSymbol iconCommunication() {
        return symbols.get("communication");
    }

    public static MaterialSymbol iconCommunities() {
        return symbols.get("communities");
    }

    public static MaterialSymbol iconCommunitiesFilled() {
        return symbols.get("communities_filled");
    }

    public static MaterialSymbol iconCommute() {
        return symbols.get("commute");
    }

    public static MaterialSymbol iconCompare() {
        return symbols.get("compare");
    }

    public static MaterialSymbol iconCompareArrows() {
        return symbols.get("compare_arrows");
    }

    public static MaterialSymbol iconCompassCalibration() {
        return symbols.get("compass_calibration");
    }

    public static MaterialSymbol iconComponentExchange() {
        return symbols.get("component_exchange");
    }

    public static MaterialSymbol iconCompost() {
        return symbols.get("compost");
    }

    public static MaterialSymbol iconCompress() {
        return symbols.get("compress");
    }

    public static MaterialSymbol iconComputer() {
        return symbols.get("computer");
    }

    public static MaterialSymbol iconComputerArrowUp() {
        return symbols.get("computer_arrow_up");
    }

    public static MaterialSymbol iconComputerCancel() {
        return symbols.get("computer_cancel");
    }

    public static MaterialSymbol iconConcierge() {
        return symbols.get("concierge");
    }

    public static MaterialSymbol iconConditions() {
        return symbols.get("conditions");
    }

    public static MaterialSymbol iconConfirmationNumber() {
        return symbols.get("confirmation_number");
    }

    public static MaterialSymbol iconCongenital() {
        return symbols.get("congenital");
    }

    public static MaterialSymbol iconConnectWithoutContact() {
        return symbols.get("connect_without_contact");
    }

    public static MaterialSymbol iconConnectedTv() {
        return symbols.get("connected_tv");
    }

    public static MaterialSymbol iconConnectingAirports() {
        return symbols.get("connecting_airports");
    }

    public static MaterialSymbol iconConstruction() {
        return symbols.get("construction");
    }

    public static MaterialSymbol iconContactEmergency() {
        return symbols.get("contact_emergency");
    }

    public static MaterialSymbol iconContactMail() {
        return symbols.get("contact_mail");
    }

    public static MaterialSymbol iconContactPage() {
        return symbols.get("contact_page");
    }

    public static MaterialSymbol iconContactPhone() {
        return symbols.get("contact_phone");
    }

    public static MaterialSymbol iconContactPhoneFilled() {
        return symbols.get("contact_phone_filled");
    }

    public static MaterialSymbol iconContactSupport() {
        return symbols.get("contact_support");
    }

    public static MaterialSymbol iconContactless() {
        return symbols.get("contactless");
    }

    public static MaterialSymbol iconContactlessOff() {
        return symbols.get("contactless_off");
    }

    public static MaterialSymbol iconContacts() {
        return symbols.get("contacts");
    }

    public static MaterialSymbol iconContactsProduct() {
        return symbols.get("contacts_product");
    }

    public static MaterialSymbol iconContentCopy() {
        return symbols.get("content_copy");
    }

    public static MaterialSymbol iconContentCut() {
        return symbols.get("content_cut");
    }

    public static MaterialSymbol iconContentPaste() {
        return symbols.get("content_paste");
    }

    public static MaterialSymbol iconContentPasteGo() {
        return symbols.get("content_paste_go");
    }

    public static MaterialSymbol iconContentPasteOff() {
        return symbols.get("content_paste_off");
    }

    public static MaterialSymbol iconContentPasteSearch() {
        return symbols.get("content_paste_search");
    }

    public static MaterialSymbol iconContextualToken() {
        return symbols.get("contextual_token");
    }

    public static MaterialSymbol iconContextualTokenAdd() {
        return symbols.get("contextual_token_add");
    }

    public static MaterialSymbol iconContract() {
        return symbols.get("contract");
    }

    public static MaterialSymbol iconContractDelete() {
        return symbols.get("contract_delete");
    }

    public static MaterialSymbol iconContractEdit() {
        return symbols.get("contract_edit");
    }

    public static MaterialSymbol iconContrast() {
        return symbols.get("contrast");
    }

    public static MaterialSymbol iconContrastCircle() {
        return symbols.get("contrast_circle");
    }

    public static MaterialSymbol iconContrastRtlOff() {
        return symbols.get("contrast_rtl_off");
    }

    public static MaterialSymbol iconContrastSquare() {
        return symbols.get("contrast_square");
    }

    public static MaterialSymbol iconControlCamera() {
        return symbols.get("control_camera");
    }

    public static MaterialSymbol iconControlPoint() {
        return symbols.get("control_point");
    }

    public static MaterialSymbol iconControlPointDuplicate() {
        return symbols.get("control_point_duplicate");
    }

    public static MaterialSymbol iconControllerGen() {
        return symbols.get("controller_gen");
    }

    public static MaterialSymbol iconConversation() {
        return symbols.get("conversation");
    }

    public static MaterialSymbol iconConversionPath() {
        return symbols.get("conversion_path");
    }

    public static MaterialSymbol iconConversionPathOff() {
        return symbols.get("conversion_path_off");
    }

    public static MaterialSymbol iconConvertToText() {
        return symbols.get("convert_to_text");
    }

    public static MaterialSymbol iconConveyorBelt() {
        return symbols.get("conveyor_belt");
    }

    public static MaterialSymbol iconCookie() {
        return symbols.get("cookie");
    }

    public static MaterialSymbol iconCookieOff() {
        return symbols.get("cookie_off");
    }

    public static MaterialSymbol iconCooking() {
        return symbols.get("cooking");
    }

    public static MaterialSymbol iconCoolToDry() {
        return symbols.get("cool_to_dry");
    }

    public static MaterialSymbol iconCopyAll() {
        return symbols.get("copy_all");
    }

    public static MaterialSymbol iconCopyright() {
        return symbols.get("copyright");
    }

    public static MaterialSymbol iconCoronavirus() {
        return symbols.get("coronavirus");
    }

    public static MaterialSymbol iconCorporateFare() {
        return symbols.get("corporate_fare");
    }

    public static MaterialSymbol iconCottage() {
        return symbols.get("cottage");
    }

    public static MaterialSymbol iconCounter0() {
        return symbols.get("counter_0");
    }

    public static MaterialSymbol iconCounter1() {
        return symbols.get("counter_1");
    }

    public static MaterialSymbol iconCounter2() {
        return symbols.get("counter_2");
    }

    public static MaterialSymbol iconCounter3() {
        return symbols.get("counter_3");
    }

    public static MaterialSymbol iconCounter4() {
        return symbols.get("counter_4");
    }

    public static MaterialSymbol iconCounter5() {
        return symbols.get("counter_5");
    }

    public static MaterialSymbol iconCounter6() {
        return symbols.get("counter_6");
    }

    public static MaterialSymbol iconCounter7() {
        return symbols.get("counter_7");
    }

    public static MaterialSymbol iconCounter8() {
        return symbols.get("counter_8");
    }

    public static MaterialSymbol iconCounter9() {
        return symbols.get("counter_9");
    }

    public static MaterialSymbol iconCountertops() {
        return symbols.get("countertops");
    }

    public static MaterialSymbol iconCreate() {
        return symbols.get("create");
    }

    public static MaterialSymbol iconCreateNewFolder() {
        return symbols.get("create_new_folder");
    }

    public static MaterialSymbol iconCreditCard() {
        return symbols.get("credit_card");
    }

    public static MaterialSymbol iconCreditCardClock() {
        return symbols.get("credit_card_clock");
    }

    public static MaterialSymbol iconCreditCardGear() {
        return symbols.get("credit_card_gear");
    }

    public static MaterialSymbol iconCreditCardHeart() {
        return symbols.get("credit_card_heart");
    }

    public static MaterialSymbol iconCreditCardOff() {
        return symbols.get("credit_card_off");
    }

    public static MaterialSymbol iconCreditScore() {
        return symbols.get("credit_score");
    }

    public static MaterialSymbol iconCrib() {
        return symbols.get("crib");
    }

    public static MaterialSymbol iconCrisisAlert() {
        return symbols.get("crisis_alert");
    }

    public static MaterialSymbol iconCrop() {
        return symbols.get("crop");
    }

    public static MaterialSymbol iconCrop169() {
        return symbols.get("crop_16_9");
    }

    public static MaterialSymbol iconCrop32() {
        return symbols.get("crop_3_2");
    }

    public static MaterialSymbol iconCrop54() {
        return symbols.get("crop_5_4");
    }

    public static MaterialSymbol iconCrop75() {
        return symbols.get("crop_7_5");
    }

    public static MaterialSymbol iconCrop916() {
        return symbols.get("crop_9_16");
    }

    public static MaterialSymbol iconCropDin() {
        return symbols.get("crop_din");
    }

    public static MaterialSymbol iconCropFree() {
        return symbols.get("crop_free");
    }

    public static MaterialSymbol iconCropLandscape() {
        return symbols.get("crop_landscape");
    }

    public static MaterialSymbol iconCropOriginal() {
        return symbols.get("crop_original");
    }

    public static MaterialSymbol iconCropPortrait() {
        return symbols.get("crop_portrait");
    }

    public static MaterialSymbol iconCropRotate() {
        return symbols.get("crop_rotate");
    }

    public static MaterialSymbol iconCropSquare() {
        return symbols.get("crop_square");
    }

    public static MaterialSymbol iconCrossword() {
        return symbols.get("crossword");
    }

    public static MaterialSymbol iconCrowdsource() {
        return symbols.get("crowdsource");
    }

    public static MaterialSymbol iconCrown() {
        return symbols.get("crown");
    }

    public static MaterialSymbol iconCrueltyFree() {
        return symbols.get("cruelty_free");
    }

    public static MaterialSymbol iconCss() {
        return symbols.get("css");
    }

    public static MaterialSymbol iconCsv() {
        return symbols.get("csv");
    }

    public static MaterialSymbol iconCurrencyBitcoin() {
        return symbols.get("currency_bitcoin");
    }

    public static MaterialSymbol iconCurrencyExchange() {
        return symbols.get("currency_exchange");
    }

    public static MaterialSymbol iconCurrencyFranc() {
        return symbols.get("currency_franc");
    }

    public static MaterialSymbol iconCurrencyLira() {
        return symbols.get("currency_lira");
    }

    public static MaterialSymbol iconCurrencyPound() {
        return symbols.get("currency_pound");
    }

    public static MaterialSymbol iconCurrencyRuble() {
        return symbols.get("currency_ruble");
    }

    public static MaterialSymbol iconCurrencyRupee() {
        return symbols.get("currency_rupee");
    }

    public static MaterialSymbol iconCurrencyRupeeCircle() {
        return symbols.get("currency_rupee_circle");
    }

    public static MaterialSymbol iconCurrencyYen() {
        return symbols.get("currency_yen");
    }

    public static MaterialSymbol iconCurrencyYuan() {
        return symbols.get("currency_yuan");
    }

    public static MaterialSymbol iconCurtains() {
        return symbols.get("curtains");
    }

    public static MaterialSymbol iconCurtainsClosed() {
        return symbols.get("curtains_closed");
    }

    public static MaterialSymbol iconCustomTypography() {
        return symbols.get("custom_typography");
    }

    public static MaterialSymbol iconCut() {
        return symbols.get("cut");
    }

    public static MaterialSymbol iconCycle() {
        return symbols.get("cycle");
    }

    public static MaterialSymbol iconCyclone() {
        return symbols.get("cyclone");
    }

    public static MaterialSymbol iconDangerous() {
        return symbols.get("dangerous");
    }

    public static MaterialSymbol iconDarkMode() {
        return symbols.get("dark_mode");
    }

    public static MaterialSymbol iconDashboard() {
        return symbols.get("dashboard");
    }

    public static MaterialSymbol iconDashboard2() {
        return symbols.get("dashboard_2");
    }

    public static MaterialSymbol iconDashboardCustomize() {
        return symbols.get("dashboard_customize");
    }

    public static MaterialSymbol iconDataAlert() {
        return symbols.get("data_alert");
    }

    public static MaterialSymbol iconDataArray() {
        return symbols.get("data_array");
    }

    public static MaterialSymbol iconDataCheck() {
        return symbols.get("data_check");
    }

    public static MaterialSymbol iconDataExploration() {
        return symbols.get("data_exploration");
    }

    public static MaterialSymbol iconDataInfoAlert() {
        return symbols.get("data_info_alert");
    }

    public static MaterialSymbol iconDataLossPrevention() {
        return symbols.get("data_loss_prevention");
    }

    public static MaterialSymbol iconDataObject() {
        return symbols.get("data_object");
    }

    public static MaterialSymbol iconDataSaverOff() {
        return symbols.get("data_saver_off");
    }

    public static MaterialSymbol iconDataSaverOn() {
        return symbols.get("data_saver_on");
    }

    public static MaterialSymbol iconDataTable() {
        return symbols.get("data_table");
    }

    public static MaterialSymbol iconDataThresholding() {
        return symbols.get("data_thresholding");
    }

    public static MaterialSymbol iconDataUsage() {
        return symbols.get("data_usage");
    }

    public static MaterialSymbol iconDatabase() {
        return symbols.get("database");
    }

    public static MaterialSymbol iconDatabaseOff() {
        return symbols.get("database_off");
    }

    public static MaterialSymbol iconDatabaseSearch() {
        return symbols.get("database_search");
    }

    public static MaterialSymbol iconDatabaseUpload() {
        return symbols.get("database_upload");
    }

    public static MaterialSymbol iconDataset() {
        return symbols.get("dataset");
    }

    public static MaterialSymbol iconDatasetLinked() {
        return symbols.get("dataset_linked");
    }

    public static MaterialSymbol iconDateRange() {
        return symbols.get("date_range");
    }

    public static MaterialSymbol iconDeblur() {
        return symbols.get("deblur");
    }

    public static MaterialSymbol iconDeceased() {
        return symbols.get("deceased");
    }

    public static MaterialSymbol iconDecimalDecrease() {
        return symbols.get("decimal_decrease");
    }

    public static MaterialSymbol iconDecimalIncrease() {
        return symbols.get("decimal_increase");
    }

    public static MaterialSymbol iconDeck() {
        return symbols.get("deck");
    }

    public static MaterialSymbol iconDehaze() {
        return symbols.get("dehaze");
    }

    public static MaterialSymbol iconDelete() {
        return symbols.get("delete");
    }

    public static MaterialSymbol iconDeleteForever() {
        return symbols.get("delete_forever");
    }

    public static MaterialSymbol iconDeleteHistory() {
        return symbols.get("delete_history");
    }

    public static MaterialSymbol iconDeleteOutline() {
        return symbols.get("delete_outline");
    }

    public static MaterialSymbol iconDeleteSweep() {
        return symbols.get("delete_sweep");
    }

    public static MaterialSymbol iconDeliveryDining() {
        return symbols.get("delivery_dining");
    }

    public static MaterialSymbol iconDeliveryTruckBolt() {
        return symbols.get("delivery_truck_bolt");
    }

    public static MaterialSymbol iconDeliveryTruckSpeed() {
        return symbols.get("delivery_truck_speed");
    }

    public static MaterialSymbol iconDemography() {
        return symbols.get("demography");
    }

    public static MaterialSymbol iconDensityLarge() {
        return symbols.get("density_large");
    }

    public static MaterialSymbol iconDensityMedium() {
        return symbols.get("density_medium");
    }

    public static MaterialSymbol iconDensitySmall() {
        return symbols.get("density_small");
    }

    public static MaterialSymbol iconDentistry() {
        return symbols.get("dentistry");
    }

    public static MaterialSymbol iconDepartureBoard() {
        return symbols.get("departure_board");
    }

    public static MaterialSymbol iconDeployedCode() {
        return symbols.get("deployed_code");
    }

    public static MaterialSymbol iconDeployedCodeAccount() {
        return symbols.get("deployed_code_account");
    }

    public static MaterialSymbol iconDeployedCodeAlert() {
        return symbols.get("deployed_code_alert");
    }

    public static MaterialSymbol iconDeployedCodeHistory() {
        return symbols.get("deployed_code_history");
    }

    public static MaterialSymbol iconDeployedCodeUpdate() {
        return symbols.get("deployed_code_update");
    }

    public static MaterialSymbol iconDermatology() {
        return symbols.get("dermatology");
    }

    public static MaterialSymbol iconDescription() {
        return symbols.get("description");
    }

    public static MaterialSymbol iconDeselect() {
        return symbols.get("deselect");
    }

    public static MaterialSymbol iconDesignServices() {
        return symbols.get("design_services");
    }

    public static MaterialSymbol iconDesk() {
        return symbols.get("desk");
    }

    public static MaterialSymbol iconDeskphone() {
        return symbols.get("deskphone");
    }

    public static MaterialSymbol iconDesktopAccessDisabled() {
        return symbols.get("desktop_access_disabled");
    }

    public static MaterialSymbol iconDesktopCloud() {
        return symbols.get("desktop_cloud");
    }

    public static MaterialSymbol iconDesktopCloudStack() {
        return symbols.get("desktop_cloud_stack");
    }

    public static MaterialSymbol iconDesktopLandscape() {
        return symbols.get("desktop_landscape");
    }

    public static MaterialSymbol iconDesktopLandscapeAdd() {
        return symbols.get("desktop_landscape_add");
    }

    public static MaterialSymbol iconDesktopMac() {
        return symbols.get("desktop_mac");
    }

    public static MaterialSymbol iconDesktopPortrait() {
        return symbols.get("desktop_portrait");
    }

    public static MaterialSymbol iconDesktopWindows() {
        return symbols.get("desktop_windows");
    }

    public static MaterialSymbol iconDestruction() {
        return symbols.get("destruction");
    }

    public static MaterialSymbol iconDetails() {
        return symbols.get("details");
    }

    public static MaterialSymbol iconDetectionAndZone() {
        return symbols.get("detection_and_zone");
    }

    public static MaterialSymbol iconDetector() {
        return symbols.get("detector");
    }

    public static MaterialSymbol iconDetectorAlarm() {
        return symbols.get("detector_alarm");
    }

    public static MaterialSymbol iconDetectorBattery() {
        return symbols.get("detector_battery");
    }

    public static MaterialSymbol iconDetectorCo() {
        return symbols.get("detector_co");
    }

    public static MaterialSymbol iconDetectorOffline() {
        return symbols.get("detector_offline");
    }

    public static MaterialSymbol iconDetectorSmoke() {
        return symbols.get("detector_smoke");
    }

    public static MaterialSymbol iconDetectorStatus() {
        return symbols.get("detector_status");
    }

    public static MaterialSymbol iconDeveloperBoard() {
        return symbols.get("developer_board");
    }

    public static MaterialSymbol iconDeveloperBoardOff() {
        return symbols.get("developer_board_off");
    }

    public static MaterialSymbol iconDeveloperGuide() {
        return symbols.get("developer_guide");
    }

    public static MaterialSymbol iconDeveloperMode() {
        return symbols.get("developer_mode");
    }

    public static MaterialSymbol iconDeveloperModeTv() {
        return symbols.get("developer_mode_tv");
    }

    public static MaterialSymbol iconDeviceBand() {
        return symbols.get("device_band");
    }

    public static MaterialSymbol iconDeviceHub() {
        return symbols.get("device_hub");
    }

    public static MaterialSymbol iconDeviceReset() {
        return symbols.get("device_reset");
    }

    public static MaterialSymbol iconDeviceThermostat() {
        return symbols.get("device_thermostat");
    }

    public static MaterialSymbol iconDeviceUnknown() {
        return symbols.get("device_unknown");
    }

    public static MaterialSymbol iconDevices() {
        return symbols.get("devices");
    }

    public static MaterialSymbol iconDevicesFold() {
        return symbols.get("devices_fold");
    }

    public static MaterialSymbol iconDevicesFold2() {
        return symbols.get("devices_fold_2");
    }

    public static MaterialSymbol iconDevicesOff() {
        return symbols.get("devices_off");
    }

    public static MaterialSymbol iconDevicesOther() {
        return symbols.get("devices_other");
    }

    public static MaterialSymbol iconDevicesWearables() {
        return symbols.get("devices_wearables");
    }

    public static MaterialSymbol iconDewPoint() {
        return symbols.get("dew_point");
    }

    public static MaterialSymbol iconDiagnosis() {
        return symbols.get("diagnosis");
    }

    public static MaterialSymbol iconDiagonalLine() {
        return symbols.get("diagonal_line");
    }

    public static MaterialSymbol iconDialerSip() {
        return symbols.get("dialer_sip");
    }

    public static MaterialSymbol iconDialogs() {
        return symbols.get("dialogs");
    }

    public static MaterialSymbol iconDialpad() {
        return symbols.get("dialpad");
    }

    public static MaterialSymbol iconDiamond() {
        return symbols.get("diamond");
    }

    public static MaterialSymbol iconDiamondShine() {
        return symbols.get("diamond_shine");
    }

    public static MaterialSymbol iconDictionary() {
        return symbols.get("dictionary");
    }

    public static MaterialSymbol iconDifference() {
        return symbols.get("difference");
    }

    public static MaterialSymbol iconDigitalOutOfHome() {
        return symbols.get("digital_out_of_home");
    }

    public static MaterialSymbol iconDigitalWellbeing() {
        return symbols.get("digital_wellbeing");
    }

    public static MaterialSymbol iconDineHeart() {
        return symbols.get("dine_heart");
    }

    public static MaterialSymbol iconDineIn() {
        return symbols.get("dine_in");
    }

    public static MaterialSymbol iconDineLamp() {
        return symbols.get("dine_lamp");
    }

    public static MaterialSymbol iconDining() {
        return symbols.get("dining");
    }

    public static MaterialSymbol iconDinnerDining() {
        return symbols.get("dinner_dining");
    }

    public static MaterialSymbol iconDirections() {
        return symbols.get("directions");
    }

    public static MaterialSymbol iconDirectionsAlt() {
        return symbols.get("directions_alt");
    }

    public static MaterialSymbol iconDirectionsAltOff() {
        return symbols.get("directions_alt_off");
    }

    public static MaterialSymbol iconDirectionsBike() {
        return symbols.get("directions_bike");
    }

    public static MaterialSymbol iconDirectionsBoat() {
        return symbols.get("directions_boat");
    }

    public static MaterialSymbol iconDirectionsBoatFilled() {
        return symbols.get("directions_boat_filled");
    }

    public static MaterialSymbol iconDirectionsBus() {
        return symbols.get("directions_bus");
    }

    public static MaterialSymbol iconDirectionsBusFilled() {
        return symbols.get("directions_bus_filled");
    }

    public static MaterialSymbol iconDirectionsCar() {
        return symbols.get("directions_car");
    }

    public static MaterialSymbol iconDirectionsCarFilled() {
        return symbols.get("directions_car_filled");
    }

    public static MaterialSymbol iconDirectionsOff() {
        return symbols.get("directions_off");
    }

    public static MaterialSymbol iconDirectionsRailway() {
        return symbols.get("directions_railway");
    }

    public static MaterialSymbol iconDirectionsRailway2() {
        return symbols.get("directions_railway_2");
    }

    public static MaterialSymbol iconDirectionsRailwayFilled() {
        return symbols.get("directions_railway_filled");
    }

    public static MaterialSymbol iconDirectionsRun() {
        return symbols.get("directions_run");
    }

    public static MaterialSymbol iconDirectionsSubway() {
        return symbols.get("directions_subway");
    }

    public static MaterialSymbol iconDirectionsSubwayFilled() {
        return symbols.get("directions_subway_filled");
    }

    public static MaterialSymbol iconDirectionsTransit() {
        return symbols.get("directions_transit");
    }

    public static MaterialSymbol iconDirectionsTransitFilled() {
        return symbols.get("directions_transit_filled");
    }

    public static MaterialSymbol iconDirectionsWalk() {
        return symbols.get("directions_walk");
    }

    public static MaterialSymbol iconDirectorySync() {
        return symbols.get("directory_sync");
    }

    public static MaterialSymbol iconDirtyLens() {
        return symbols.get("dirty_lens");
    }

    public static MaterialSymbol iconDisabledByDefault() {
        return symbols.get("disabled_by_default");
    }

    public static MaterialSymbol iconDisabledVisible() {
        return symbols.get("disabled_visible");
    }

    public static MaterialSymbol iconDiscFull() {
        return symbols.get("disc_full");
    }

    public static MaterialSymbol iconDiscoverTune() {
        return symbols.get("discover_tune");
    }

    public static MaterialSymbol iconDishwasher() {
        return symbols.get("dishwasher");
    }

    public static MaterialSymbol iconDishwasherGen() {
        return symbols.get("dishwasher_gen");
    }

    public static MaterialSymbol iconDisplayExternalInput() {
        return symbols.get("display_external_input");
    }

    public static MaterialSymbol iconDisplaySettings() {
        return symbols.get("display_settings");
    }

    public static MaterialSymbol iconDistance() {
        return symbols.get("distance");
    }

    public static MaterialSymbol iconDiversity1() {
        return symbols.get("diversity_1");
    }

    public static MaterialSymbol iconDiversity2() {
        return symbols.get("diversity_2");
    }

    public static MaterialSymbol iconDiversity3() {
        return symbols.get("diversity_3");
    }

    public static MaterialSymbol iconDiversity4() {
        return symbols.get("diversity_4");
    }

    public static MaterialSymbol iconDns() {
        return symbols.get("dns");
    }

    public static MaterialSymbol iconDoDisturb() {
        return symbols.get("do_disturb");
    }

    public static MaterialSymbol iconDoDisturbAlt() {
        return symbols.get("do_disturb_alt");
    }

    public static MaterialSymbol iconDoDisturbOff() {
        return symbols.get("do_disturb_off");
    }

    public static MaterialSymbol iconDoDisturbOn() {
        return symbols.get("do_disturb_on");
    }

    public static MaterialSymbol iconDoNotDisturb() {
        return symbols.get("do_not_disturb");
    }

    public static MaterialSymbol iconDoNotDisturbAlt() {
        return symbols.get("do_not_disturb_alt");
    }

    public static MaterialSymbol iconDoNotDisturbOff() {
        return symbols.get("do_not_disturb_off");
    }

    public static MaterialSymbol iconDoNotDisturbOn() {
        return symbols.get("do_not_disturb_on");
    }

    public static MaterialSymbol iconDoNotDisturbOnTotalSilence() {
        return symbols.get("do_not_disturb_on_total_silence");
    }

    public static MaterialSymbol iconDoNotStep() {
        return symbols.get("do_not_step");
    }

    public static MaterialSymbol iconDoNotTouch() {
        return symbols.get("do_not_touch");
    }

    public static MaterialSymbol iconDock() {
        return symbols.get("dock");
    }

    public static MaterialSymbol iconDockToBottom() {
        return symbols.get("dock_to_bottom");
    }

    public static MaterialSymbol iconDockToLeft() {
        return symbols.get("dock_to_left");
    }

    public static MaterialSymbol iconDockToRight() {
        return symbols.get("dock_to_right");
    }

    public static MaterialSymbol iconDocs() {
        return symbols.get("docs");
    }

    public static MaterialSymbol iconDocsAddOn() {
        return symbols.get("docs_add_on");
    }

    public static MaterialSymbol iconDocsAppsScript() {
        return symbols.get("docs_apps_script");
    }

    public static MaterialSymbol iconDocumentScanner() {
        return symbols.get("document_scanner");
    }

    public static MaterialSymbol iconDocumentSearch() {
        return symbols.get("document_search");
    }

    public static MaterialSymbol iconDomain() {
        return symbols.get("domain");
    }

    public static MaterialSymbol iconDomainAdd() {
        return symbols.get("domain_add");
    }

    public static MaterialSymbol iconDomainDisabled() {
        return symbols.get("domain_disabled");
    }

    public static MaterialSymbol iconDomainVerification() {
        return symbols.get("domain_verification");
    }

    public static MaterialSymbol iconDomainVerificationOff() {
        return symbols.get("domain_verification_off");
    }

    public static MaterialSymbol iconDominoMask() {
        return symbols.get("domino_mask");
    }

    public static MaterialSymbol iconDone() {
        return symbols.get("done");
    }

    public static MaterialSymbol iconDoneAll() {
        return symbols.get("done_all");
    }

    public static MaterialSymbol iconDoneOutline() {
        return symbols.get("done_outline");
    }

    public static MaterialSymbol iconDonutLarge() {
        return symbols.get("donut_large");
    }

    public static MaterialSymbol iconDonutSmall() {
        return symbols.get("donut_small");
    }

    public static MaterialSymbol iconDoorBack() {
        return symbols.get("door_back");
    }

    public static MaterialSymbol iconDoorFront() {
        return symbols.get("door_front");
    }

    public static MaterialSymbol iconDoorOpen() {
        return symbols.get("door_open");
    }

    public static MaterialSymbol iconDoorSensor() {
        return symbols.get("door_sensor");
    }

    public static MaterialSymbol iconDoorSliding() {
        return symbols.get("door_sliding");
    }

    public static MaterialSymbol iconDoorbell() {
        return symbols.get("doorbell");
    }

    public static MaterialSymbol iconDoorbell3p() {
        return symbols.get("doorbell_3p");
    }

    public static MaterialSymbol iconDoorbellChime() {
        return symbols.get("doorbell_chime");
    }

    public static MaterialSymbol iconDoubleArrow() {
        return symbols.get("double_arrow");
    }

    public static MaterialSymbol iconDownhillSkiing() {
        return symbols.get("downhill_skiing");
    }

    public static MaterialSymbol iconDownload() {
        return symbols.get("download");
    }

    public static MaterialSymbol iconDownload2() {
        return symbols.get("download_2");
    }

    public static MaterialSymbol iconDownloadDone() {
        return symbols.get("download_done");
    }

    public static MaterialSymbol iconDownloadForOffline() {
        return symbols.get("download_for_offline");
    }

    public static MaterialSymbol iconDownloading() {
        return symbols.get("downloading");
    }

    public static MaterialSymbol iconDraft() {
        return symbols.get("draft");
    }

    public static MaterialSymbol iconDraftOrders() {
        return symbols.get("draft_orders");
    }

    public static MaterialSymbol iconDrafts() {
        return symbols.get("drafts");
    }

    public static MaterialSymbol iconDragClick() {
        return symbols.get("drag_click");
    }

    public static MaterialSymbol iconDragHandle() {
        return symbols.get("drag_handle");
    }

    public static MaterialSymbol iconDragIndicator() {
        return symbols.get("drag_indicator");
    }

    public static MaterialSymbol iconDragPan() {
        return symbols.get("drag_pan");
    }

    public static MaterialSymbol iconDraw() {
        return symbols.get("draw");
    }

    public static MaterialSymbol iconDrawAbstract() {
        return symbols.get("draw_abstract");
    }

    public static MaterialSymbol iconDrawCollage() {
        return symbols.get("draw_collage");
    }

    public static MaterialSymbol iconDrawingRecognition() {
        return symbols.get("drawing_recognition");
    }

    public static MaterialSymbol iconDresser() {
        return symbols.get("dresser");
    }

    public static MaterialSymbol iconDriveEta() {
        return symbols.get("drive_eta");
    }

    public static MaterialSymbol iconDriveExport() {
        return symbols.get("drive_export");
    }

    public static MaterialSymbol iconDriveFileMove() {
        return symbols.get("drive_file_move");
    }

    public static MaterialSymbol iconDriveFileMoveOutline() {
        return symbols.get("drive_file_move_outline");
    }

    public static MaterialSymbol iconDriveFileMoveRtl() {
        return symbols.get("drive_file_move_rtl");
    }

    public static MaterialSymbol iconDriveFileRenameOutline() {
        return symbols.get("drive_file_rename_outline");
    }

    public static MaterialSymbol iconDriveFolderUpload() {
        return symbols.get("drive_folder_upload");
    }

    public static MaterialSymbol iconDriveFusiontable() {
        return symbols.get("drive_fusiontable");
    }

    public static MaterialSymbol iconDrone() {
        return symbols.get("drone");
    }

    public static MaterialSymbol iconDrone2() {
        return symbols.get("drone_2");
    }

    public static MaterialSymbol iconDropdown() {
        return symbols.get("dropdown");
    }

    public static MaterialSymbol iconDropperEye() {
        return symbols.get("dropper_eye");
    }

    public static MaterialSymbol iconDry() {
        return symbols.get("dry");
    }

    public static MaterialSymbol iconDryCleaning() {
        return symbols.get("dry_cleaning");
    }

    public static MaterialSymbol iconDualScreen() {
        return symbols.get("dual_screen");
    }

    public static MaterialSymbol iconDuo() {
        return symbols.get("duo");
    }

    public static MaterialSymbol iconDvr() {
        return symbols.get("dvr");
    }

    public static MaterialSymbol iconDynamicFeed() {
        return symbols.get("dynamic_feed");
    }

    public static MaterialSymbol iconDynamicForm() {
        return symbols.get("dynamic_form");
    }

    public static MaterialSymbol iconE911Avatar() {
        return symbols.get("e911_avatar");
    }

    public static MaterialSymbol iconE911Emergency() {
        return symbols.get("e911_emergency");
    }

    public static MaterialSymbol iconEMobiledata() {
        return symbols.get("e_mobiledata");
    }

    public static MaterialSymbol iconEMobiledataBadge() {
        return symbols.get("e_mobiledata_badge");
    }

    public static MaterialSymbol iconEarSound() {
        return symbols.get("ear_sound");
    }

    public static MaterialSymbol iconEarbudCase() {
        return symbols.get("earbud_case");
    }

    public static MaterialSymbol iconEarbudLeft() {
        return symbols.get("earbud_left");
    }

    public static MaterialSymbol iconEarbudRight() {
        return symbols.get("earbud_right");
    }

    public static MaterialSymbol iconEarbuds() {
        return symbols.get("earbuds");
    }

    public static MaterialSymbol iconEarbuds2() {
        return symbols.get("earbuds_2");
    }

    public static MaterialSymbol iconEarbudsBattery() {
        return symbols.get("earbuds_battery");
    }

    public static MaterialSymbol iconEarlyOn() {
        return symbols.get("early_on");
    }

    public static MaterialSymbol iconEarthquake() {
        return symbols.get("earthquake");
    }

    public static MaterialSymbol iconEast() {
        return symbols.get("east");
    }

    public static MaterialSymbol iconEcg() {
        return symbols.get("ecg");
    }

    public static MaterialSymbol iconEcgHeart() {
        return symbols.get("ecg_heart");
    }

    public static MaterialSymbol iconEco() {
        return symbols.get("eco");
    }

    public static MaterialSymbol iconEda() {
        return symbols.get("eda");
    }

    public static MaterialSymbol iconEdgesensorHigh() {
        return symbols.get("edgesensor_high");
    }

    public static MaterialSymbol iconEdgesensorLow() {
        return symbols.get("edgesensor_low");
    }

    public static MaterialSymbol iconEdit() {
        return symbols.get("edit");
    }

    public static MaterialSymbol iconEditArrowDown() {
        return symbols.get("edit_arrow_down");
    }

    public static MaterialSymbol iconEditArrowUp() {
        return symbols.get("edit_arrow_up");
    }

    public static MaterialSymbol iconEditAttributes() {
        return symbols.get("edit_attributes");
    }

    public static MaterialSymbol iconEditAudio() {
        return symbols.get("edit_audio");
    }

    public static MaterialSymbol iconEditCalendar() {
        return symbols.get("edit_calendar");
    }

    public static MaterialSymbol iconEditDocument() {
        return symbols.get("edit_document");
    }

    public static MaterialSymbol iconEditLocation() {
        return symbols.get("edit_location");
    }

    public static MaterialSymbol iconEditLocationAlt() {
        return symbols.get("edit_location_alt");
    }

    public static MaterialSymbol iconEditNote() {
        return symbols.get("edit_note");
    }

    public static MaterialSymbol iconEditNotifications() {
        return symbols.get("edit_notifications");
    }

    public static MaterialSymbol iconEditOff() {
        return symbols.get("edit_off");
    }

    public static MaterialSymbol iconEditRoad() {
        return symbols.get("edit_road");
    }

    public static MaterialSymbol iconEditSquare() {
        return symbols.get("edit_square");
    }

    public static MaterialSymbol iconEditorChoice() {
        return symbols.get("editor_choice");
    }

    public static MaterialSymbol iconEgg() {
        return symbols.get("egg");
    }

    public static MaterialSymbol iconEggAlt() {
        return symbols.get("egg_alt");
    }

    public static MaterialSymbol iconEject() {
        return symbols.get("eject");
    }

    public static MaterialSymbol iconElderly() {
        return symbols.get("elderly");
    }

    public static MaterialSymbol iconElderlyWoman() {
        return symbols.get("elderly_woman");
    }

    public static MaterialSymbol iconElectricBike() {
        return symbols.get("electric_bike");
    }

    public static MaterialSymbol iconElectricBolt() {
        return symbols.get("electric_bolt");
    }

    public static MaterialSymbol iconElectricCar() {
        return symbols.get("electric_car");
    }

    public static MaterialSymbol iconElectricMeter() {
        return symbols.get("electric_meter");
    }

    public static MaterialSymbol iconElectricMoped() {
        return symbols.get("electric_moped");
    }

    public static MaterialSymbol iconElectricRickshaw() {
        return symbols.get("electric_rickshaw");
    }

    public static MaterialSymbol iconElectricScooter() {
        return symbols.get("electric_scooter");
    }

    public static MaterialSymbol iconElectricalServices() {
        return symbols.get("electrical_services");
    }

    public static MaterialSymbol iconElevation() {
        return symbols.get("elevation");
    }

    public static MaterialSymbol iconElevator() {
        return symbols.get("elevator");
    }

    public static MaterialSymbol iconEmail() {
        return symbols.get("email");
    }

    public static MaterialSymbol iconEmergency() {
        return symbols.get("emergency");
    }

    public static MaterialSymbol iconEmergencyHeat() {
        return symbols.get("emergency_heat");
    }

    public static MaterialSymbol iconEmergencyHeat2() {
        return symbols.get("emergency_heat_2");
    }

    public static MaterialSymbol iconEmergencyHome() {
        return symbols.get("emergency_home");
    }

    public static MaterialSymbol iconEmergencyRecording() {
        return symbols.get("emergency_recording");
    }

    public static MaterialSymbol iconEmergencyShare() {
        return symbols.get("emergency_share");
    }

    public static MaterialSymbol iconEmergencyShareOff() {
        return symbols.get("emergency_share_off");
    }

    public static MaterialSymbol iconEmojiEmotions() {
        return symbols.get("emoji_emotions");
    }

    public static MaterialSymbol iconEmojiEvents() {
        return symbols.get("emoji_events");
    }

    public static MaterialSymbol iconEmojiFlags() {
        return symbols.get("emoji_flags");
    }

    public static MaterialSymbol iconEmojiFoodBeverage() {
        return symbols.get("emoji_food_beverage");
    }

    public static MaterialSymbol iconEmojiLanguage() {
        return symbols.get("emoji_language");
    }

    public static MaterialSymbol iconEmojiNature() {
        return symbols.get("emoji_nature");
    }

    public static MaterialSymbol iconEmojiObjects() {
        return symbols.get("emoji_objects");
    }

    public static MaterialSymbol iconEmojiPeople() {
        return symbols.get("emoji_people");
    }

    public static MaterialSymbol iconEmojiSymbols() {
        return symbols.get("emoji_symbols");
    }

    public static MaterialSymbol iconEmojiTransportation() {
        return symbols.get("emoji_transportation");
    }

    public static MaterialSymbol iconEmoticon() {
        return symbols.get("emoticon");
    }

    public static MaterialSymbol iconEmptyDashboard() {
        return symbols.get("empty_dashboard");
    }

    public static MaterialSymbol iconEnable() {
        return symbols.get("enable");
    }

    public static MaterialSymbol iconEncrypted() {
        return symbols.get("encrypted");
    }

    public static MaterialSymbol iconEncryptedAdd() {
        return symbols.get("encrypted_add");
    }

    public static MaterialSymbol iconEncryptedAddCircle() {
        return symbols.get("encrypted_add_circle");
    }

    public static MaterialSymbol iconEncryptedMinusCircle() {
        return symbols.get("encrypted_minus_circle");
    }

    public static MaterialSymbol iconEncryptedOff() {
        return symbols.get("encrypted_off");
    }

    public static MaterialSymbol iconEndocrinology() {
        return symbols.get("endocrinology");
    }

    public static MaterialSymbol iconEnergy() {
        return symbols.get("energy");
    }

    public static MaterialSymbol iconEnergyProgramSaving() {
        return symbols.get("energy_program_saving");
    }

    public static MaterialSymbol iconEnergyProgramTimeUsed() {
        return symbols.get("energy_program_time_used");
    }

    public static MaterialSymbol iconEnergySavingsLeaf() {
        return symbols.get("energy_savings_leaf");
    }

    public static MaterialSymbol iconEngineering() {
        return symbols.get("engineering");
    }

    public static MaterialSymbol iconEnhancedEncryption() {
        return symbols.get("enhanced_encryption");
    }

    public static MaterialSymbol iconEnt() {
        return symbols.get("ent");
    }

    public static MaterialSymbol iconEnterprise() {
        return symbols.get("enterprise");
    }

    public static MaterialSymbol iconEnterpriseOff() {
        return symbols.get("enterprise_off");
    }

    public static MaterialSymbol iconEqual() {
        return symbols.get("equal");
    }

    public static MaterialSymbol iconEqualizer() {
        return symbols.get("equalizer");
    }

    public static MaterialSymbol iconEraserSize1() {
        return symbols.get("eraser_size_1");
    }

    public static MaterialSymbol iconEraserSize2() {
        return symbols.get("eraser_size_2");
    }

    public static MaterialSymbol iconEraserSize3() {
        return symbols.get("eraser_size_3");
    }

    public static MaterialSymbol iconEraserSize4() {
        return symbols.get("eraser_size_4");
    }

    public static MaterialSymbol iconEraserSize5() {
        return symbols.get("eraser_size_5");
    }

    public static MaterialSymbol iconError() {
        return symbols.get("error");
    }

    public static MaterialSymbol iconErrorCircleRounded() {
        return symbols.get("error_circle_rounded");
    }

    public static MaterialSymbol iconErrorMed() {
        return symbols.get("error_med");
    }

    public static MaterialSymbol iconErrorOutline() {
        return symbols.get("error_outline");
    }

    public static MaterialSymbol iconEscalator() {
        return symbols.get("escalator");
    }

    public static MaterialSymbol iconEscalatorWarning() {
        return symbols.get("escalator_warning");
    }

    public static MaterialSymbol iconEuro() {
        return symbols.get("euro");
    }

    public static MaterialSymbol iconEuroSymbol() {
        return symbols.get("euro_symbol");
    }

    public static MaterialSymbol iconEvCharger() {
        return symbols.get("ev_charger");
    }

    public static MaterialSymbol iconEvMobiledataBadge() {
        return symbols.get("ev_mobiledata_badge");
    }

    public static MaterialSymbol iconEvShadow() {
        return symbols.get("ev_shadow");
    }

    public static MaterialSymbol iconEvShadowAdd() {
        return symbols.get("ev_shadow_add");
    }

    public static MaterialSymbol iconEvShadowMinus() {
        return symbols.get("ev_shadow_minus");
    }

    public static MaterialSymbol iconEvStation() {
        return symbols.get("ev_station");
    }

    public static MaterialSymbol iconEvent() {
        return symbols.get("event");
    }

    public static MaterialSymbol iconEventAvailable() {
        return symbols.get("event_available");
    }

    public static MaterialSymbol iconEventBusy() {
        return symbols.get("event_busy");
    }

    public static MaterialSymbol iconEventList() {
        return symbols.get("event_list");
    }

    public static MaterialSymbol iconEventNote() {
        return symbols.get("event_note");
    }

    public static MaterialSymbol iconEventRepeat() {
        return symbols.get("event_repeat");
    }

    public static MaterialSymbol iconEventSeat() {
        return symbols.get("event_seat");
    }

    public static MaterialSymbol iconEventUpcoming() {
        return symbols.get("event_upcoming");
    }

    public static MaterialSymbol iconExclamation() {
        return symbols.get("exclamation");
    }

    public static MaterialSymbol iconExercise() {
        return symbols.get("exercise");
    }

    public static MaterialSymbol iconExitToApp() {
        return symbols.get("exit_to_app");
    }

    public static MaterialSymbol iconExpand() {
        return symbols.get("expand");
    }

    public static MaterialSymbol iconExpandAll() {
        return symbols.get("expand_all");
    }

    public static MaterialSymbol iconExpandCircleDown() {
        return symbols.get("expand_circle_down");
    }

    public static MaterialSymbol iconExpandCircleRight() {
        return symbols.get("expand_circle_right");
    }

    public static MaterialSymbol iconExpandCircleUp() {
        return symbols.get("expand_circle_up");
    }

    public static MaterialSymbol iconExpandContent() {
        return symbols.get("expand_content");
    }

    public static MaterialSymbol iconExpandLess() {
        return symbols.get("expand_less");
    }

    public static MaterialSymbol iconExpandMore() {
        return symbols.get("expand_more");
    }

    public static MaterialSymbol iconExpansionPanels() {
        return symbols.get("expansion_panels");
    }

    public static MaterialSymbol iconExpensionPanels() {
        return symbols.get("expension_panels");
    }

    public static MaterialSymbol iconExperiment() {
        return symbols.get("experiment");
    }

    public static MaterialSymbol iconExplicit() {
        return symbols.get("explicit");
    }

    public static MaterialSymbol iconExplore() {
        return symbols.get("explore");
    }

    public static MaterialSymbol iconExploreNearby() {
        return symbols.get("explore_nearby");
    }

    public static MaterialSymbol iconExploreOff() {
        return symbols.get("explore_off");
    }

    public static MaterialSymbol iconExplosion() {
        return symbols.get("explosion");
    }

    public static MaterialSymbol iconExportNotes() {
        return symbols.get("export_notes");
    }

    public static MaterialSymbol iconExposure() {
        return symbols.get("exposure");
    }

    public static MaterialSymbol iconExposureNeg1() {
        return symbols.get("exposure_neg_1");
    }

    public static MaterialSymbol iconExposureNeg2() {
        return symbols.get("exposure_neg_2");
    }

    public static MaterialSymbol iconExposurePlus1() {
        return symbols.get("exposure_plus_1");
    }

    public static MaterialSymbol iconExposurePlus2() {
        return symbols.get("exposure_plus_2");
    }

    public static MaterialSymbol iconExposureZero() {
        return symbols.get("exposure_zero");
    }

    public static MaterialSymbol iconExtension() {
        return symbols.get("extension");
    }

    public static MaterialSymbol iconExtensionOff() {
        return symbols.get("extension_off");
    }

    public static MaterialSymbol iconEyeTracking() {
        return symbols.get("eye_tracking");
    }

    public static MaterialSymbol iconEyeglasses() {
        return symbols.get("eyeglasses");
    }

    public static MaterialSymbol iconEyeglasses2() {
        return symbols.get("eyeglasses_2");
    }

    public static MaterialSymbol iconEyeglasses2Sound() {
        return symbols.get("eyeglasses_2_sound");
    }

    public static MaterialSymbol iconFace() {
        return symbols.get("face");
    }

    public static MaterialSymbol iconFace2() {
        return symbols.get("face_2");
    }

    public static MaterialSymbol iconFace3() {
        return symbols.get("face_3");
    }

    public static MaterialSymbol iconFace4() {
        return symbols.get("face_4");
    }

    public static MaterialSymbol iconFace5() {
        return symbols.get("face_5");
    }

    public static MaterialSymbol iconFace6() {
        return symbols.get("face_6");
    }

    public static MaterialSymbol iconFaceDown() {
        return symbols.get("face_down");
    }

    public static MaterialSymbol iconFaceLeft() {
        return symbols.get("face_left");
    }

    public static MaterialSymbol iconFaceNod() {
        return symbols.get("face_nod");
    }

    public static MaterialSymbol iconFaceRetouchingNatural() {
        return symbols.get("face_retouching_natural");
    }

    public static MaterialSymbol iconFaceRetouchingOff() {
        return symbols.get("face_retouching_off");
    }

    public static MaterialSymbol iconFaceRight() {
        return symbols.get("face_right");
    }

    public static MaterialSymbol iconFaceShake() {
        return symbols.get("face_shake");
    }

    public static MaterialSymbol iconFaceUnlock() {
        return symbols.get("face_unlock");
    }

    public static MaterialSymbol iconFaceUp() {
        return symbols.get("face_up");
    }

    public static MaterialSymbol iconFactCheck() {
        return symbols.get("fact_check");
    }

    public static MaterialSymbol iconFactory() {
        return symbols.get("factory");
    }

    public static MaterialSymbol iconFalling() {
        return symbols.get("falling");
    }

    public static MaterialSymbol iconFamiliarFaceAndZone() {
        return symbols.get("familiar_face_and_zone");
    }

    public static MaterialSymbol iconFamilyGroup() {
        return symbols.get("family_group");
    }

    public static MaterialSymbol iconFamilyHistory() {
        return symbols.get("family_history");
    }

    public static MaterialSymbol iconFamilyHome() {
        return symbols.get("family_home");
    }

    public static MaterialSymbol iconFamilyLink() {
        return symbols.get("family_link");
    }

    public static MaterialSymbol iconFamilyRestroom() {
        return symbols.get("family_restroom");
    }

    public static MaterialSymbol iconFamilyStar() {
        return symbols.get("family_star");
    }

    public static MaterialSymbol iconFanFocus() {
        return symbols.get("fan_focus");
    }

    public static MaterialSymbol iconFanIndirect() {
        return symbols.get("fan_indirect");
    }

    public static MaterialSymbol iconFarsightDigital() {
        return symbols.get("farsight_digital");
    }

    public static MaterialSymbol iconFastForward() {
        return symbols.get("fast_forward");
    }

    public static MaterialSymbol iconFastRewind() {
        return symbols.get("fast_rewind");
    }

    public static MaterialSymbol iconFastfood() {
        return symbols.get("fastfood");
    }

    public static MaterialSymbol iconFaucet() {
        return symbols.get("faucet");
    }

    public static MaterialSymbol iconFavorite() {
        return symbols.get("favorite");
    }

    public static MaterialSymbol iconFavoriteBorder() {
        return symbols.get("favorite_border");
    }

    public static MaterialSymbol iconFax() {
        return symbols.get("fax");
    }

    public static MaterialSymbol iconFeatureSearch() {
        return symbols.get("feature_search");
    }

    public static MaterialSymbol iconFeaturedPlayList() {
        return symbols.get("featured_play_list");
    }

    public static MaterialSymbol iconFeaturedSeasonalAndGifts() {
        return symbols.get("featured_seasonal_and_gifts");
    }

    public static MaterialSymbol iconFeaturedVideo() {
        return symbols.get("featured_video");
    }

    public static MaterialSymbol iconFeed() {
        return symbols.get("feed");
    }

    public static MaterialSymbol iconFeedback() {
        return symbols.get("feedback");
    }

    public static MaterialSymbol iconFemale() {
        return symbols.get("female");
    }

    public static MaterialSymbol iconFemur() {
        return symbols.get("femur");
    }

    public static MaterialSymbol iconFemurAlt() {
        return symbols.get("femur_alt");
    }

    public static MaterialSymbol iconFence() {
        return symbols.get("fence");
    }

    public static MaterialSymbol iconFertile() {
        return symbols.get("fertile");
    }

    public static MaterialSymbol iconFestival() {
        return symbols.get("festival");
    }

    public static MaterialSymbol iconFiberDvr() {
        return symbols.get("fiber_dvr");
    }

    public static MaterialSymbol iconFiberManualRecord() {
        return symbols.get("fiber_manual_record");
    }

    public static MaterialSymbol iconFiberNew() {
        return symbols.get("fiber_new");
    }

    public static MaterialSymbol iconFiberPin() {
        return symbols.get("fiber_pin");
    }

    public static MaterialSymbol iconFiberSmartRecord() {
        return symbols.get("fiber_smart_record");
    }

    public static MaterialSymbol iconFileCopy() {
        return symbols.get("file_copy");
    }

    public static MaterialSymbol iconFileCopyOff() {
        return symbols.get("file_copy_off");
    }

    public static MaterialSymbol iconFileDownload() {
        return symbols.get("file_download");
    }

    public static MaterialSymbol iconFileDownloadDone() {
        return symbols.get("file_download_done");
    }

    public static MaterialSymbol iconFileDownloadOff() {
        return symbols.get("file_download_off");
    }

    public static MaterialSymbol iconFileExport() {
        return symbols.get("file_export");
    }

    public static MaterialSymbol iconFileJson() {
        return symbols.get("file_json");
    }

    public static MaterialSymbol iconFileMap() {
        return symbols.get("file_map");
    }

    public static MaterialSymbol iconFileMapStack() {
        return symbols.get("file_map_stack");
    }

    public static MaterialSymbol iconFileOpen() {
        return symbols.get("file_open");
    }

    public static MaterialSymbol iconFilePng() {
        return symbols.get("file_png");
    }

    public static MaterialSymbol iconFilePresent() {
        return symbols.get("file_present");
    }

    public static MaterialSymbol iconFileSave() {
        return symbols.get("file_save");
    }

    public static MaterialSymbol iconFileSaveOff() {
        return symbols.get("file_save_off");
    }

    public static MaterialSymbol iconFileUpload() {
        return symbols.get("file_upload");
    }

    public static MaterialSymbol iconFileUploadOff() {
        return symbols.get("file_upload_off");
    }

    public static MaterialSymbol iconFiles() {
        return symbols.get("files");
    }

    public static MaterialSymbol iconFilter() {
        return symbols.get("filter");
    }

    public static MaterialSymbol iconFilter1() {
        return symbols.get("filter_1");
    }

    public static MaterialSymbol iconFilter2() {
        return symbols.get("filter_2");
    }

    public static MaterialSymbol iconFilter3() {
        return symbols.get("filter_3");
    }

    public static MaterialSymbol iconFilter4() {
        return symbols.get("filter_4");
    }

    public static MaterialSymbol iconFilter5() {
        return symbols.get("filter_5");
    }

    public static MaterialSymbol iconFilter6() {
        return symbols.get("filter_6");
    }

    public static MaterialSymbol iconFilter7() {
        return symbols.get("filter_7");
    }

    public static MaterialSymbol iconFilter8() {
        return symbols.get("filter_8");
    }

    public static MaterialSymbol iconFilter9() {
        return symbols.get("filter_9");
    }

    public static MaterialSymbol iconFilter9Plus() {
        return symbols.get("filter_9_plus");
    }

    public static MaterialSymbol iconFilterAlt() {
        return symbols.get("filter_alt");
    }

    public static MaterialSymbol iconFilterAltOff() {
        return symbols.get("filter_alt_off");
    }

    public static MaterialSymbol iconFilterArrowRight() {
        return symbols.get("filter_arrow_right");
    }

    public static MaterialSymbol iconFilterBAndW() {
        return symbols.get("filter_b_and_w");
    }

    public static MaterialSymbol iconFilterCenterFocus() {
        return symbols.get("filter_center_focus");
    }

    public static MaterialSymbol iconFilterDrama() {
        return symbols.get("filter_drama");
    }

    public static MaterialSymbol iconFilterFrames() {
        return symbols.get("filter_frames");
    }

    public static MaterialSymbol iconFilterHdr() {
        return symbols.get("filter_hdr");
    }

    public static MaterialSymbol iconFilterList() {
        return symbols.get("filter_list");
    }

    public static MaterialSymbol iconFilterListAlt() {
        return symbols.get("filter_list_alt");
    }

    public static MaterialSymbol iconFilterListOff() {
        return symbols.get("filter_list_off");
    }

    public static MaterialSymbol iconFilterNone() {
        return symbols.get("filter_none");
    }

    public static MaterialSymbol iconFilterRetrolux() {
        return symbols.get("filter_retrolux");
    }

    public static MaterialSymbol iconFilterTiltShift() {
        return symbols.get("filter_tilt_shift");
    }

    public static MaterialSymbol iconFilterVintage() {
        return symbols.get("filter_vintage");
    }

    public static MaterialSymbol iconFinance() {
        return symbols.get("finance");
    }

    public static MaterialSymbol iconFinanceChip() {
        return symbols.get("finance_chip");
    }

    public static MaterialSymbol iconFinanceMode() {
        return symbols.get("finance_mode");
    }

    public static MaterialSymbol iconFindInPage() {
        return symbols.get("find_in_page");
    }

    public static MaterialSymbol iconFindReplace() {
        return symbols.get("find_replace");
    }

    public static MaterialSymbol iconFingerprint() {
        return symbols.get("fingerprint");
    }

    public static MaterialSymbol iconFingerprintOff() {
        return symbols.get("fingerprint_off");
    }

    public static MaterialSymbol iconFireExtinguisher() {
        return symbols.get("fire_extinguisher");
    }

    public static MaterialSymbol iconFireHydrant() {
        return symbols.get("fire_hydrant");
    }

    public static MaterialSymbol iconFireTruck() {
        return symbols.get("fire_truck");
    }

    public static MaterialSymbol iconFireplace() {
        return symbols.get("fireplace");
    }

    public static MaterialSymbol iconFirstPage() {
        return symbols.get("first_page");
    }

    public static MaterialSymbol iconFitPage() {
        return symbols.get("fit_page");
    }

    public static MaterialSymbol iconFitPageHeight() {
        return symbols.get("fit_page_height");
    }

    public static MaterialSymbol iconFitPageWidth() {
        return symbols.get("fit_page_width");
    }

    public static MaterialSymbol iconFitScreen() {
        return symbols.get("fit_screen");
    }

    public static MaterialSymbol iconFitWidth() {
        return symbols.get("fit_width");
    }

    public static MaterialSymbol iconFitnessCenter() {
        return symbols.get("fitness_center");
    }

    public static MaterialSymbol iconFitnessTracker() {
        return symbols.get("fitness_tracker");
    }

    public static MaterialSymbol iconFitnessTrackers() {
        return symbols.get("fitness_trackers");
    }

    public static MaterialSymbol iconFlag() {
        return symbols.get("flag");
    }

    public static MaterialSymbol iconFlag2() {
        return symbols.get("flag_2");
    }

    public static MaterialSymbol iconFlagCheck() {
        return symbols.get("flag_check");
    }

    public static MaterialSymbol iconFlagCircle() {
        return symbols.get("flag_circle");
    }

    public static MaterialSymbol iconFlagFilled() {
        return symbols.get("flag_filled");
    }

    public static MaterialSymbol iconFlaky() {
        return symbols.get("flaky");
    }

    public static MaterialSymbol iconFlare() {
        return symbols.get("flare");
    }

    public static MaterialSymbol iconFlashAuto() {
        return symbols.get("flash_auto");
    }

    public static MaterialSymbol iconFlashOff() {
        return symbols.get("flash_off");
    }

    public static MaterialSymbol iconFlashOn() {
        return symbols.get("flash_on");
    }

    public static MaterialSymbol iconFlashlightOff() {
        return symbols.get("flashlight_off");
    }

    public static MaterialSymbol iconFlashlightOn() {
        return symbols.get("flashlight_on");
    }

    public static MaterialSymbol iconFlatware() {
        return symbols.get("flatware");
    }

    public static MaterialSymbol iconFlexDirection() {
        return symbols.get("flex_direction");
    }

    public static MaterialSymbol iconFlexNoWrap() {
        return symbols.get("flex_no_wrap");
    }

    public static MaterialSymbol iconFlexWrap() {
        return symbols.get("flex_wrap");
    }

    public static MaterialSymbol iconFlight() {
        return symbols.get("flight");
    }

    public static MaterialSymbol iconFlightClass() {
        return symbols.get("flight_class");
    }

    public static MaterialSymbol iconFlightLand() {
        return symbols.get("flight_land");
    }

    public static MaterialSymbol iconFlightTakeoff() {
        return symbols.get("flight_takeoff");
    }

    public static MaterialSymbol iconFlightsAndHotels() {
        return symbols.get("flights_and_hotels");
    }

    public static MaterialSymbol iconFlightsmode() {
        return symbols.get("flightsmode");
    }

    public static MaterialSymbol iconFlip() {
        return symbols.get("flip");
    }

    public static MaterialSymbol iconFlipCameraAndroid() {
        return symbols.get("flip_camera_android");
    }

    public static MaterialSymbol iconFlipCameraIos() {
        return symbols.get("flip_camera_ios");
    }

    public static MaterialSymbol iconFlipToBack() {
        return symbols.get("flip_to_back");
    }

    public static MaterialSymbol iconFlipToFront() {
        return symbols.get("flip_to_front");
    }

    public static MaterialSymbol iconFloatLandscape2() {
        return symbols.get("float_landscape_2");
    }

    public static MaterialSymbol iconFloatPortrait2() {
        return symbols.get("float_portrait_2");
    }

    public static MaterialSymbol iconFlood() {
        return symbols.get("flood");
    }

    public static MaterialSymbol iconFloor() {
        return symbols.get("floor");
    }

    public static MaterialSymbol iconFloorLamp() {
        return symbols.get("floor_lamp");
    }

    public static MaterialSymbol iconFlourescent() {
        return symbols.get("flourescent");
    }

    public static MaterialSymbol iconFlowchart() {
        return symbols.get("flowchart");
    }

    public static MaterialSymbol iconFlowsheet() {
        return symbols.get("flowsheet");
    }

    public static MaterialSymbol iconFluid() {
        return symbols.get("fluid");
    }

    public static MaterialSymbol iconFluidBalance() {
        return symbols.get("fluid_balance");
    }

    public static MaterialSymbol iconFluidMed() {
        return symbols.get("fluid_med");
    }

    public static MaterialSymbol iconFluorescent() {
        return symbols.get("fluorescent");
    }

    public static MaterialSymbol iconFlutter() {
        return symbols.get("flutter");
    }

    public static MaterialSymbol iconFlutterDash() {
        return symbols.get("flutter_dash");
    }

    public static MaterialSymbol iconFlyover() {
        return symbols.get("flyover");
    }

    public static MaterialSymbol iconFmdBad() {
        return symbols.get("fmd_bad");
    }

    public static MaterialSymbol iconFmdGood() {
        return symbols.get("fmd_good");
    }

    public static MaterialSymbol iconFoggy() {
        return symbols.get("foggy");
    }

    public static MaterialSymbol iconFoldedHands() {
        return symbols.get("folded_hands");
    }

    public static MaterialSymbol iconFolder() {
        return symbols.get("folder");
    }

    public static MaterialSymbol iconFolderCheck() {
        return symbols.get("folder_check");
    }

    public static MaterialSymbol iconFolderCheck2() {
        return symbols.get("folder_check_2");
    }

    public static MaterialSymbol iconFolderCode() {
        return symbols.get("folder_code");
    }

    public static MaterialSymbol iconFolderCopy() {
        return symbols.get("folder_copy");
    }

    public static MaterialSymbol iconFolderData() {
        return symbols.get("folder_data");
    }

    public static MaterialSymbol iconFolderDelete() {
        return symbols.get("folder_delete");
    }

    public static MaterialSymbol iconFolderEye() {
        return symbols.get("folder_eye");
    }

    public static MaterialSymbol iconFolderInfo() {
        return symbols.get("folder_info");
    }

    public static MaterialSymbol iconFolderLimited() {
        return symbols.get("folder_limited");
    }

    public static MaterialSymbol iconFolderManaged() {
        return symbols.get("folder_managed");
    }

    public static MaterialSymbol iconFolderMatch() {
        return symbols.get("folder_match");
    }

    public static MaterialSymbol iconFolderOff() {
        return symbols.get("folder_off");
    }

    public static MaterialSymbol iconFolderOpen() {
        return symbols.get("folder_open");
    }

    public static MaterialSymbol iconFolderShared() {
        return symbols.get("folder_shared");
    }

    public static MaterialSymbol iconFolderSpecial() {
        return symbols.get("folder_special");
    }

    public static MaterialSymbol iconFolderSupervised() {
        return symbols.get("folder_supervised");
    }

    public static MaterialSymbol iconFolderZip() {
        return symbols.get("folder_zip");
    }

    public static MaterialSymbol iconFollowTheSigns() {
        return symbols.get("follow_the_signs");
    }

    public static MaterialSymbol iconFontDownload() {
        return symbols.get("font_download");
    }

    public static MaterialSymbol iconFontDownloadOff() {
        return symbols.get("font_download_off");
    }

    public static MaterialSymbol iconFoodBank() {
        return symbols.get("food_bank");
    }

    public static MaterialSymbol iconFootBones() {
        return symbols.get("foot_bones");
    }

    public static MaterialSymbol iconFootprint() {
        return symbols.get("footprint");
    }

    public static MaterialSymbol iconForYou() {
        return symbols.get("for_you");
    }

    public static MaterialSymbol iconForest() {
        return symbols.get("forest");
    }

    public static MaterialSymbol iconForkLeft() {
        return symbols.get("fork_left");
    }

    public static MaterialSymbol iconForkRight() {
        return symbols.get("fork_right");
    }

    public static MaterialSymbol iconForkSpoon() {
        return symbols.get("fork_spoon");
    }

    public static MaterialSymbol iconForklift() {
        return symbols.get("forklift");
    }

    public static MaterialSymbol iconFormatAlignCenter() {
        return symbols.get("format_align_center");
    }

    public static MaterialSymbol iconFormatAlignJustify() {
        return symbols.get("format_align_justify");
    }

    public static MaterialSymbol iconFormatAlignLeft() {
        return symbols.get("format_align_left");
    }

    public static MaterialSymbol iconFormatAlignRight() {
        return symbols.get("format_align_right");
    }

    public static MaterialSymbol iconFormatBold() {
        return symbols.get("format_bold");
    }

    public static MaterialSymbol iconFormatClear() {
        return symbols.get("format_clear");
    }

    public static MaterialSymbol iconFormatColorFill() {
        return symbols.get("format_color_fill");
    }

    public static MaterialSymbol iconFormatColorReset() {
        return symbols.get("format_color_reset");
    }

    public static MaterialSymbol iconFormatColorText() {
        return symbols.get("format_color_text");
    }

    public static MaterialSymbol iconFormatH1() {
        return symbols.get("format_h1");
    }

    public static MaterialSymbol iconFormatH2() {
        return symbols.get("format_h2");
    }

    public static MaterialSymbol iconFormatH3() {
        return symbols.get("format_h3");
    }

    public static MaterialSymbol iconFormatH4() {
        return symbols.get("format_h4");
    }

    public static MaterialSymbol iconFormatH5() {
        return symbols.get("format_h5");
    }

    public static MaterialSymbol iconFormatH6() {
        return symbols.get("format_h6");
    }

    public static MaterialSymbol iconFormatImageLeft() {
        return symbols.get("format_image_left");
    }

    public static MaterialSymbol iconFormatImageRight() {
        return symbols.get("format_image_right");
    }

    public static MaterialSymbol iconFormatIndentDecrease() {
        return symbols.get("format_indent_decrease");
    }

    public static MaterialSymbol iconFormatIndentIncrease() {
        return symbols.get("format_indent_increase");
    }

    public static MaterialSymbol iconFormatInkHighlighter() {
        return symbols.get("format_ink_highlighter");
    }

    public static MaterialSymbol iconFormatItalic() {
        return symbols.get("format_italic");
    }

    public static MaterialSymbol iconFormatLetterSpacing() {
        return symbols.get("format_letter_spacing");
    }

    public static MaterialSymbol iconFormatLetterSpacing2() {
        return symbols.get("format_letter_spacing_2");
    }

    public static MaterialSymbol iconFormatLetterSpacingStandard() {
        return symbols.get("format_letter_spacing_standard");
    }

    public static MaterialSymbol iconFormatLetterSpacingWide() {
        return symbols.get("format_letter_spacing_wide");
    }

    public static MaterialSymbol iconFormatLetterSpacingWider() {
        return symbols.get("format_letter_spacing_wider");
    }

    public static MaterialSymbol iconFormatLineSpacing() {
        return symbols.get("format_line_spacing");
    }

    public static MaterialSymbol iconFormatListBulleted() {
        return symbols.get("format_list_bulleted");
    }

    public static MaterialSymbol iconFormatListBulletedAdd() {
        return symbols.get("format_list_bulleted_add");
    }

    public static MaterialSymbol iconFormatListNumbered() {
        return symbols.get("format_list_numbered");
    }

    public static MaterialSymbol iconFormatListNumberedRtl() {
        return symbols.get("format_list_numbered_rtl");
    }

    public static MaterialSymbol iconFormatOverline() {
        return symbols.get("format_overline");
    }

    public static MaterialSymbol iconFormatPaint() {
        return symbols.get("format_paint");
    }

    public static MaterialSymbol iconFormatParagraph() {
        return symbols.get("format_paragraph");
    }

    public static MaterialSymbol iconFormatQuote() {
        return symbols.get("format_quote");
    }

    public static MaterialSymbol iconFormatQuoteOff() {
        return symbols.get("format_quote_off");
    }

    public static MaterialSymbol iconFormatShapes() {
        return symbols.get("format_shapes");
    }

    public static MaterialSymbol iconFormatSize() {
        return symbols.get("format_size");
    }

    public static MaterialSymbol iconFormatStrikethrough() {
        return symbols.get("format_strikethrough");
    }

    public static MaterialSymbol iconFormatTextClip() {
        return symbols.get("format_text_clip");
    }

    public static MaterialSymbol iconFormatTextOverflow() {
        return symbols.get("format_text_overflow");
    }

    public static MaterialSymbol iconFormatTextWrap() {
        return symbols.get("format_text_wrap");
    }

    public static MaterialSymbol iconFormatTextdirectionLToR() {
        return symbols.get("format_textdirection_l_to_r");
    }

    public static MaterialSymbol iconFormatTextdirectionRToL() {
        return symbols.get("format_textdirection_r_to_l");
    }

    public static MaterialSymbol iconFormatTextdirectionVertical() {
        return symbols.get("format_textdirection_vertical");
    }

    public static MaterialSymbol iconFormatUnderlined() {
        return symbols.get("format_underlined");
    }

    public static MaterialSymbol iconFormatUnderlinedSquiggle() {
        return symbols.get("format_underlined_squiggle");
    }

    public static MaterialSymbol iconFormsAddOn() {
        return symbols.get("forms_add_on");
    }

    public static MaterialSymbol iconFormsAppsScript() {
        return symbols.get("forms_apps_script");
    }

    public static MaterialSymbol iconFort() {
        return symbols.get("fort");
    }

    public static MaterialSymbol iconForum() {
        return symbols.get("forum");
    }

    public static MaterialSymbol iconForward() {
        return symbols.get("forward");
    }

    public static MaterialSymbol iconForward10() {
        return symbols.get("forward_10");
    }

    public static MaterialSymbol iconForward30() {
        return symbols.get("forward_30");
    }

    public static MaterialSymbol iconForward5() {
        return symbols.get("forward_5");
    }

    public static MaterialSymbol iconForwardCircle() {
        return symbols.get("forward_circle");
    }

    public static MaterialSymbol iconForwardMedia() {
        return symbols.get("forward_media");
    }

    public static MaterialSymbol iconForwardToInbox() {
        return symbols.get("forward_to_inbox");
    }

    public static MaterialSymbol iconFoundation() {
        return symbols.get("foundation");
    }

    public static MaterialSymbol iconFragrance() {
        return symbols.get("fragrance");
    }

    public static MaterialSymbol iconFrameBug() {
        return symbols.get("frame_bug");
    }

    public static MaterialSymbol iconFrameExclamation() {
        return symbols.get("frame_exclamation");
    }

    public static MaterialSymbol iconFrameInspect() {
        return symbols.get("frame_inspect");
    }

    public static MaterialSymbol iconFramePerson() {
        return symbols.get("frame_person");
    }

    public static MaterialSymbol iconFramePersonMic() {
        return symbols.get("frame_person_mic");
    }

    public static MaterialSymbol iconFramePersonOff() {
        return symbols.get("frame_person_off");
    }

    public static MaterialSymbol iconFrameReload() {
        return symbols.get("frame_reload");
    }

    public static MaterialSymbol iconFrameSource() {
        return symbols.get("frame_source");
    }

    public static MaterialSymbol iconFreeBreakfast() {
        return symbols.get("free_breakfast");
    }

    public static MaterialSymbol iconFreeCancellation() {
        return symbols.get("free_cancellation");
    }

    public static MaterialSymbol iconFrontHand() {
        return symbols.get("front_hand");
    }

    public static MaterialSymbol iconFrontLoader() {
        return symbols.get("front_loader");
    }

    public static MaterialSymbol iconFullCoverage() {
        return symbols.get("full_coverage");
    }

    public static MaterialSymbol iconFullHd() {
        return symbols.get("full_hd");
    }

    public static MaterialSymbol iconFullStackedBarChart() {
        return symbols.get("full_stacked_bar_chart");
    }

    public static MaterialSymbol iconFullscreen() {
        return symbols.get("fullscreen");
    }

    public static MaterialSymbol iconFullscreenExit() {
        return symbols.get("fullscreen_exit");
    }

    public static MaterialSymbol iconFullscreenPortrait() {
        return symbols.get("fullscreen_portrait");
    }

    public static MaterialSymbol iconFunction() {
        return symbols.get("function");
    }

    public static MaterialSymbol iconFunctions() {
        return symbols.get("functions");
    }

    public static MaterialSymbol iconFunicular() {
        return symbols.get("funicular");
    }

    public static MaterialSymbol iconGMobiledata() {
        return symbols.get("g_mobiledata");
    }

    public static MaterialSymbol iconGMobiledataBadge() {
        return symbols.get("g_mobiledata_badge");
    }

    public static MaterialSymbol iconGTranslate() {
        return symbols.get("g_translate");
    }

    public static MaterialSymbol iconGalleryThumbnail() {
        return symbols.get("gallery_thumbnail");
    }

    public static MaterialSymbol iconGamepad() {
        return symbols.get("gamepad");
    }

    public static MaterialSymbol iconGames() {
        return symbols.get("games");
    }

    public static MaterialSymbol iconGarage() {
        return symbols.get("garage");
    }

    public static MaterialSymbol iconGarageCheck() {
        return symbols.get("garage_check");
    }

    public static MaterialSymbol iconGarageDoor() {
        return symbols.get("garage_door");
    }

    public static MaterialSymbol iconGarageHome() {
        return symbols.get("garage_home");
    }

    public static MaterialSymbol iconGarageMoney() {
        return symbols.get("garage_money");
    }

    public static MaterialSymbol iconGardenCart() {
        return symbols.get("garden_cart");
    }

    public static MaterialSymbol iconGasMeter() {
        return symbols.get("gas_meter");
    }

    public static MaterialSymbol iconGastroenterology() {
        return symbols.get("gastroenterology");
    }

    public static MaterialSymbol iconGate() {
        return symbols.get("gate");
    }

    public static MaterialSymbol iconGavel() {
        return symbols.get("gavel");
    }

    public static MaterialSymbol iconGeneralDevice() {
        return symbols.get("general_device");
    }

    public static MaterialSymbol iconGeneratingTokens() {
        return symbols.get("generating_tokens");
    }

    public static MaterialSymbol iconGenetics() {
        return symbols.get("genetics");
    }

    public static MaterialSymbol iconGenres() {
        return symbols.get("genres");
    }

    public static MaterialSymbol iconGesture() {
        return symbols.get("gesture");
    }

    public static MaterialSymbol iconGestureSelect() {
        return symbols.get("gesture_select");
    }

    public static MaterialSymbol iconGetApp() {
        return symbols.get("get_app");
    }

    public static MaterialSymbol iconGif() {
        return symbols.get("gif");
    }

    public static MaterialSymbol iconGif2() {
        return symbols.get("gif_2");
    }

    public static MaterialSymbol iconGifBox() {
        return symbols.get("gif_box");
    }

    public static MaterialSymbol iconGirl() {
        return symbols.get("girl");
    }

    public static MaterialSymbol iconGite() {
        return symbols.get("gite");
    }

    public static MaterialSymbol iconGlassCup() {
        return symbols.get("glass_cup");
    }

    public static MaterialSymbol iconGlobe() {
        return symbols.get("globe");
    }

    public static MaterialSymbol iconGlobeAsia() {
        return symbols.get("globe_asia");
    }

    public static MaterialSymbol iconGlobeBook() {
        return symbols.get("globe_book");
    }

    public static MaterialSymbol iconGlobeLocationPin() {
        return symbols.get("globe_location_pin");
    }

    public static MaterialSymbol iconGlobeUk() {
        return symbols.get("globe_uk");
    }

    public static MaterialSymbol iconGlucose() {
        return symbols.get("glucose");
    }

    public static MaterialSymbol iconGlyphs() {
        return symbols.get("glyphs");
    }

    public static MaterialSymbol iconGoToLine() {
        return symbols.get("go_to_line");
    }

    public static MaterialSymbol iconGolfCourse() {
        return symbols.get("golf_course");
    }

    public static MaterialSymbol iconGondolaLift() {
        return symbols.get("gondola_lift");
    }

    public static MaterialSymbol iconGoogleHomeDevices() {
        return symbols.get("google_home_devices");
    }

    public static MaterialSymbol iconGooglePlusReshare() {
        return symbols.get("google_plus_reshare");
    }

    public static MaterialSymbol iconGoogleTvRemote() {
        return symbols.get("google_tv_remote");
    }

    public static MaterialSymbol iconGoogleWifi() {
        return symbols.get("google_wifi");
    }

    public static MaterialSymbol iconGppBad() {
        return symbols.get("gpp_bad");
    }

    public static MaterialSymbol iconGppGood() {
        return symbols.get("gpp_good");
    }

    public static MaterialSymbol iconGppMaybe() {
        return symbols.get("gpp_maybe");
    }

    public static MaterialSymbol iconGpsFixed() {
        return symbols.get("gps_fixed");
    }

    public static MaterialSymbol iconGpsNotFixed() {
        return symbols.get("gps_not_fixed");
    }

    public static MaterialSymbol iconGpsOff() {
        return symbols.get("gps_off");
    }

    public static MaterialSymbol iconGrade() {
        return symbols.get("grade");
    }

    public static MaterialSymbol iconGradient() {
        return symbols.get("gradient");
    }

    public static MaterialSymbol iconGrading() {
        return symbols.get("grading");
    }

    public static MaterialSymbol iconGrain() {
        return symbols.get("grain");
    }

    public static MaterialSymbol iconGraph1() {
        return symbols.get("graph_1");
    }

    public static MaterialSymbol iconGraph2() {
        return symbols.get("graph_2");
    }

    public static MaterialSymbol iconGraph3() {
        return symbols.get("graph_3");
    }

    public static MaterialSymbol iconGraph4() {
        return symbols.get("graph_4");
    }

    public static MaterialSymbol iconGraph5() {
        return symbols.get("graph_5");
    }

    public static MaterialSymbol iconGraph6() {
        return symbols.get("graph_6");
    }

    public static MaterialSymbol iconGraph7() {
        return symbols.get("graph_7");
    }

    public static MaterialSymbol iconGraphicEq() {
        return symbols.get("graphic_eq");
    }

    public static MaterialSymbol iconGrass() {
        return symbols.get("grass");
    }

    public static MaterialSymbol iconGrid3x3() {
        return symbols.get("grid_3x3");
    }

    public static MaterialSymbol iconGrid3x3Off() {
        return symbols.get("grid_3x3_off");
    }

    public static MaterialSymbol iconGrid4x4() {
        return symbols.get("grid_4x4");
    }

    public static MaterialSymbol iconGridGoldenratio() {
        return symbols.get("grid_goldenratio");
    }

    public static MaterialSymbol iconGridGuides() {
        return symbols.get("grid_guides");
    }

    public static MaterialSymbol iconGridOff() {
        return symbols.get("grid_off");
    }

    public static MaterialSymbol iconGridOn() {
        return symbols.get("grid_on");
    }

    public static MaterialSymbol iconGridView() {
        return symbols.get("grid_view");
    }

    public static MaterialSymbol iconGrocery() {
        return symbols.get("grocery");
    }

    public static MaterialSymbol iconGroup() {
        return symbols.get("group");
    }

    public static MaterialSymbol iconGroupAdd() {
        return symbols.get("group_add");
    }

    public static MaterialSymbol iconGroupOff() {
        return symbols.get("group_off");
    }

    public static MaterialSymbol iconGroupRemove() {
        return symbols.get("group_remove");
    }

    public static MaterialSymbol iconGroupSearch() {
        return symbols.get("group_search");
    }

    public static MaterialSymbol iconGroupWork() {
        return symbols.get("group_work");
    }

    public static MaterialSymbol iconGroupedBarChart() {
        return symbols.get("grouped_bar_chart");
    }

    public static MaterialSymbol iconGroups() {
        return symbols.get("groups");
    }

    public static MaterialSymbol iconGroups2() {
        return symbols.get("groups_2");
    }

    public static MaterialSymbol iconGroups3() {
        return symbols.get("groups_3");
    }

    public static MaterialSymbol iconGuardian() {
        return symbols.get("guardian");
    }

    public static MaterialSymbol iconGynecology() {
        return symbols.get("gynecology");
    }

    public static MaterialSymbol iconHMobiledata() {
        return symbols.get("h_mobiledata");
    }

    public static MaterialSymbol iconHMobiledataBadge() {
        return symbols.get("h_mobiledata_badge");
    }

    public static MaterialSymbol iconHPlusMobiledata() {
        return symbols.get("h_plus_mobiledata");
    }

    public static MaterialSymbol iconHPlusMobiledataBadge() {
        return symbols.get("h_plus_mobiledata_badge");
    }

    public static MaterialSymbol iconHail() {
        return symbols.get("hail");
    }

    public static MaterialSymbol iconHallway() {
        return symbols.get("hallway");
    }

    public static MaterialSymbol iconHanamiDango() {
        return symbols.get("hanami_dango");
    }

    public static MaterialSymbol iconHandBones() {
        return symbols.get("hand_bones");
    }

    public static MaterialSymbol iconHandGesture() {
        return symbols.get("hand_gesture");
    }

    public static MaterialSymbol iconHandGestureOff() {
        return symbols.get("hand_gesture_off");
    }

    public static MaterialSymbol iconHandMeal() {
        return symbols.get("hand_meal");
    }

    public static MaterialSymbol iconHandPackage() {
        return symbols.get("hand_package");
    }

    public static MaterialSymbol iconHandheldController() {
        return symbols.get("handheld_controller");
    }

    public static MaterialSymbol iconHandshake() {
        return symbols.get("handshake");
    }

    public static MaterialSymbol iconHandwritingRecognition() {
        return symbols.get("handwriting_recognition");
    }

    public static MaterialSymbol iconHandyman() {
        return symbols.get("handyman");
    }

    public static MaterialSymbol iconHangoutVideo() {
        return symbols.get("hangout_video");
    }

    public static MaterialSymbol iconHangoutVideoOff() {
        return symbols.get("hangout_video_off");
    }

    public static MaterialSymbol iconHardDisk() {
        return symbols.get("hard_disk");
    }

    public static MaterialSymbol iconHardDrive() {
        return symbols.get("hard_drive");
    }

    public static MaterialSymbol iconHardDrive2() {
        return symbols.get("hard_drive_2");
    }

    public static MaterialSymbol iconHardware() {
        return symbols.get("hardware");
    }

    public static MaterialSymbol iconHd() {
        return symbols.get("hd");
    }

    public static MaterialSymbol iconHdrAuto() {
        return symbols.get("hdr_auto");
    }

    public static MaterialSymbol iconHdrAutoSelect() {
        return symbols.get("hdr_auto_select");
    }

    public static MaterialSymbol iconHdrEnhancedSelect() {
        return symbols.get("hdr_enhanced_select");
    }

    public static MaterialSymbol iconHdrOff() {
        return symbols.get("hdr_off");
    }

    public static MaterialSymbol iconHdrOffSelect() {
        return symbols.get("hdr_off_select");
    }

    public static MaterialSymbol iconHdrOn() {
        return symbols.get("hdr_on");
    }

    public static MaterialSymbol iconHdrOnSelect() {
        return symbols.get("hdr_on_select");
    }

    public static MaterialSymbol iconHdrPlus() {
        return symbols.get("hdr_plus");
    }

    public static MaterialSymbol iconHdrPlusOff() {
        return symbols.get("hdr_plus_off");
    }

    public static MaterialSymbol iconHdrStrong() {
        return symbols.get("hdr_strong");
    }

    public static MaterialSymbol iconHdrWeak() {
        return symbols.get("hdr_weak");
    }

    public static MaterialSymbol iconHeadMountedDevice() {
        return symbols.get("head_mounted_device");
    }

    public static MaterialSymbol iconHeadphones() {
        return symbols.get("headphones");
    }

    public static MaterialSymbol iconHeadphonesBattery() {
        return symbols.get("headphones_battery");
    }

    public static MaterialSymbol iconHeadset() {
        return symbols.get("headset");
    }

    public static MaterialSymbol iconHeadsetMic() {
        return symbols.get("headset_mic");
    }

    public static MaterialSymbol iconHeadsetOff() {
        return symbols.get("headset_off");
    }

    public static MaterialSymbol iconHealing() {
        return symbols.get("healing");
    }

    public static MaterialSymbol iconHealthAndBeauty() {
        return symbols.get("health_and_beauty");
    }

    public static MaterialSymbol iconHealthAndSafety() {
        return symbols.get("health_and_safety");
    }

    public static MaterialSymbol iconHealthCross() {
        return symbols.get("health_cross");
    }

    public static MaterialSymbol iconHealthMetrics() {
        return symbols.get("health_metrics");
    }

    public static MaterialSymbol iconHeapSnapshotLarge() {
        return symbols.get("heap_snapshot_large");
    }

    public static MaterialSymbol iconHeapSnapshotMultiple() {
        return symbols.get("heap_snapshot_multiple");
    }

    public static MaterialSymbol iconHeapSnapshotThumbnail() {
        return symbols.get("heap_snapshot_thumbnail");
    }

    public static MaterialSymbol iconHearing() {
        return symbols.get("hearing");
    }

    public static MaterialSymbol iconHearingAid() {
        return symbols.get("hearing_aid");
    }

    public static MaterialSymbol iconHearingAidDisabled() {
        return symbols.get("hearing_aid_disabled");
    }

    public static MaterialSymbol iconHearingAidDisabledLeft() {
        return symbols.get("hearing_aid_disabled_left");
    }

    public static MaterialSymbol iconHearingAidLeft() {
        return symbols.get("hearing_aid_left");
    }

    public static MaterialSymbol iconHearingDisabled() {
        return symbols.get("hearing_disabled");
    }

    public static MaterialSymbol iconHeartBroken() {
        return symbols.get("heart_broken");
    }

    public static MaterialSymbol iconHeartCheck() {
        return symbols.get("heart_check");
    }

    public static MaterialSymbol iconHeartMinus() {
        return symbols.get("heart_minus");
    }

    public static MaterialSymbol iconHeartPlus() {
        return symbols.get("heart_plus");
    }

    public static MaterialSymbol iconHeartSmile() {
        return symbols.get("heart_smile");
    }

    public static MaterialSymbol iconHeat() {
        return symbols.get("heat");
    }

    public static MaterialSymbol iconHeatPump() {
        return symbols.get("heat_pump");
    }

    public static MaterialSymbol iconHeatPumpBalance() {
        return symbols.get("heat_pump_balance");
    }

    public static MaterialSymbol iconHeight() {
        return symbols.get("height");
    }

    public static MaterialSymbol iconHelicopter() {
        return symbols.get("helicopter");
    }

    public static MaterialSymbol iconHelp() {
        return symbols.get("help");
    }

    public static MaterialSymbol iconHelpCenter() {
        return symbols.get("help_center");
    }

    public static MaterialSymbol iconHelpClinic() {
        return symbols.get("help_clinic");
    }

    public static MaterialSymbol iconHelpOutline() {
        return symbols.get("help_outline");
    }

    public static MaterialSymbol iconHematology() {
        return symbols.get("hematology");
    }

    public static MaterialSymbol iconHevc() {
        return symbols.get("hevc");
    }

    public static MaterialSymbol iconHexagon() {
        return symbols.get("hexagon");
    }

    public static MaterialSymbol iconHide() {
        return symbols.get("hide");
    }

    public static MaterialSymbol iconHideImage() {
        return symbols.get("hide_image");
    }

    public static MaterialSymbol iconHideSource() {
        return symbols.get("hide_source");
    }

    public static MaterialSymbol iconHighChair() {
        return symbols.get("high_chair");
    }

    public static MaterialSymbol iconHighDensity() {
        return symbols.get("high_density");
    }

    public static MaterialSymbol iconHighQuality() {
        return symbols.get("high_quality");
    }

    public static MaterialSymbol iconHighRes() {
        return symbols.get("high_res");
    }

    public static MaterialSymbol iconHighlight() {
        return symbols.get("highlight");
    }

    public static MaterialSymbol iconHighlightAlt() {
        return symbols.get("highlight_alt");
    }

    public static MaterialSymbol iconHighlightKeyboardFocus() {
        return symbols.get("highlight_keyboard_focus");
    }

    public static MaterialSymbol iconHighlightMouseCursor() {
        return symbols.get("highlight_mouse_cursor");
    }

    public static MaterialSymbol iconHighlightOff() {
        return symbols.get("highlight_off");
    }

    public static MaterialSymbol iconHighlightTextCursor() {
        return symbols.get("highlight_text_cursor");
    }

    public static MaterialSymbol iconHighlighterSize1() {
        return symbols.get("highlighter_size_1");
    }

    public static MaterialSymbol iconHighlighterSize2() {
        return symbols.get("highlighter_size_2");
    }

    public static MaterialSymbol iconHighlighterSize3() {
        return symbols.get("highlighter_size_3");
    }

    public static MaterialSymbol iconHighlighterSize4() {
        return symbols.get("highlighter_size_4");
    }

    public static MaterialSymbol iconHighlighterSize5() {
        return symbols.get("highlighter_size_5");
    }

    public static MaterialSymbol iconHiking() {
        return symbols.get("hiking");
    }

    public static MaterialSymbol iconHistory() {
        return symbols.get("history");
    }

    public static MaterialSymbol iconHistory2() {
        return symbols.get("history_2");
    }

    public static MaterialSymbol iconHistoryEdu() {
        return symbols.get("history_edu");
    }

    public static MaterialSymbol iconHistoryOff() {
        return symbols.get("history_off");
    }

    public static MaterialSymbol iconHistoryToggleOff() {
        return symbols.get("history_toggle_off");
    }

    public static MaterialSymbol iconHive() {
        return symbols.get("hive");
    }

    public static MaterialSymbol iconHls() {
        return symbols.get("hls");
    }

    public static MaterialSymbol iconHlsOff() {
        return symbols.get("hls_off");
    }

    public static MaterialSymbol iconHolidayVillage() {
        return symbols.get("holiday_village");
    }

    public static MaterialSymbol iconHome() {
        return symbols.get("home");
    }

    public static MaterialSymbol iconHomeAndGarden() {
        return symbols.get("home_and_garden");
    }

    public static MaterialSymbol iconHomeAppLogo() {
        return symbols.get("home_app_logo");
    }

    public static MaterialSymbol iconHomeFilled() {
        return symbols.get("home_filled");
    }

    public static MaterialSymbol iconHomeHealth() {
        return symbols.get("home_health");
    }

    public static MaterialSymbol iconHomeImprovementAndTools() {
        return symbols.get("home_improvement_and_tools");
    }

    public static MaterialSymbol iconHomeIotDevice() {
        return symbols.get("home_iot_device");
    }

    public static MaterialSymbol iconHomeMax() {
        return symbols.get("home_max");
    }

    public static MaterialSymbol iconHomeMaxDots() {
        return symbols.get("home_max_dots");
    }

    public static MaterialSymbol iconHomeMini() {
        return symbols.get("home_mini");
    }

    public static MaterialSymbol iconHomePin() {
        return symbols.get("home_pin");
    }

    public static MaterialSymbol iconHomeRepairService() {
        return symbols.get("home_repair_service");
    }

    public static MaterialSymbol iconHomeSpeaker() {
        return symbols.get("home_speaker");
    }

    public static MaterialSymbol iconHomeStorage() {
        return symbols.get("home_storage");
    }

    public static MaterialSymbol iconHomeWork() {
        return symbols.get("home_work");
    }

    public static MaterialSymbol iconHorizontalDistribute() {
        return symbols.get("horizontal_distribute");
    }

    public static MaterialSymbol iconHorizontalRule() {
        return symbols.get("horizontal_rule");
    }

    public static MaterialSymbol iconHorizontalSplit() {
        return symbols.get("horizontal_split");
    }

    public static MaterialSymbol iconHost() {
        return symbols.get("host");
    }

    public static MaterialSymbol iconHotTub() {
        return symbols.get("hot_tub");
    }

    public static MaterialSymbol iconHotel() {
        return symbols.get("hotel");
    }

    public static MaterialSymbol iconHotelClass() {
        return symbols.get("hotel_class");
    }

    public static MaterialSymbol iconHourglass() {
        return symbols.get("hourglass");
    }

    public static MaterialSymbol iconHourglassArrowDown() {
        return symbols.get("hourglass_arrow_down");
    }

    public static MaterialSymbol iconHourglassArrowUp() {
        return symbols.get("hourglass_arrow_up");
    }

    public static MaterialSymbol iconHourglassBottom() {
        return symbols.get("hourglass_bottom");
    }

    public static MaterialSymbol iconHourglassDisabled() {
        return symbols.get("hourglass_disabled");
    }

    public static MaterialSymbol iconHourglassEmpty() {
        return symbols.get("hourglass_empty");
    }

    public static MaterialSymbol iconHourglassFull() {
        return symbols.get("hourglass_full");
    }

    public static MaterialSymbol iconHourglassPause() {
        return symbols.get("hourglass_pause");
    }

    public static MaterialSymbol iconHourglassTop() {
        return symbols.get("hourglass_top");
    }

    public static MaterialSymbol iconHouse() {
        return symbols.get("house");
    }

    public static MaterialSymbol iconHouseSiding() {
        return symbols.get("house_siding");
    }

    public static MaterialSymbol iconHouseWithShield() {
        return symbols.get("house_with_shield");
    }

    public static MaterialSymbol iconHouseboat() {
        return symbols.get("houseboat");
    }

    public static MaterialSymbol iconHouseholdSupplies() {
        return symbols.get("household_supplies");
    }

    public static MaterialSymbol iconHov() {
        return symbols.get("hov");
    }

    public static MaterialSymbol iconHowToReg() {
        return symbols.get("how_to_reg");
    }

    public static MaterialSymbol iconHowToVote() {
        return symbols.get("how_to_vote");
    }

    public static MaterialSymbol iconHrResting() {
        return symbols.get("hr_resting");
    }

    public static MaterialSymbol iconHtml() {
        return symbols.get("html");
    }

    public static MaterialSymbol iconHttp() {
        return symbols.get("http");
    }

    public static MaterialSymbol iconHttps() {
        return symbols.get("https");
    }

    public static MaterialSymbol iconHub() {
        return symbols.get("hub");
    }

    public static MaterialSymbol iconHumerus() {
        return symbols.get("humerus");
    }

    public static MaterialSymbol iconHumerusAlt() {
        return symbols.get("humerus_alt");
    }

    public static MaterialSymbol iconHumidityHigh() {
        return symbols.get("humidity_high");
    }

    public static MaterialSymbol iconHumidityIndoor() {
        return symbols.get("humidity_indoor");
    }

    public static MaterialSymbol iconHumidityLow() {
        return symbols.get("humidity_low");
    }

    public static MaterialSymbol iconHumidityMid() {
        return symbols.get("humidity_mid");
    }

    public static MaterialSymbol iconHumidityPercentage() {
        return symbols.get("humidity_percentage");
    }

    public static MaterialSymbol iconHvac() {
        return symbols.get("hvac");
    }

    public static MaterialSymbol iconHvacMaxDefrost() {
        return symbols.get("hvac_max_defrost");
    }

    public static MaterialSymbol iconIceSkating() {
        return symbols.get("ice_skating");
    }

    public static MaterialSymbol iconIcecream() {
        return symbols.get("icecream");
    }

    public static MaterialSymbol iconIdCard() {
        return symbols.get("id_card");
    }

    public static MaterialSymbol iconIdentityAwareProxy() {
        return symbols.get("identity_aware_proxy");
    }

    public static MaterialSymbol iconIdentityPlatform() {
        return symbols.get("identity_platform");
    }

    public static MaterialSymbol iconIfl() {
        return symbols.get("ifl");
    }

    public static MaterialSymbol iconIframe() {
        return symbols.get("iframe");
    }

    public static MaterialSymbol iconIframeOff() {
        return symbols.get("iframe_off");
    }

    public static MaterialSymbol iconImage() {
        return symbols.get("image");
    }

    public static MaterialSymbol iconImageArrowUp() {
        return symbols.get("image_arrow_up");
    }

    public static MaterialSymbol iconImageAspectRatio() {
        return symbols.get("image_aspect_ratio");
    }

    public static MaterialSymbol iconImageInset() {
        return symbols.get("image_inset");
    }

    public static MaterialSymbol iconImageNotSupported() {
        return symbols.get("image_not_supported");
    }

    public static MaterialSymbol iconImageSearch() {
        return symbols.get("image_search");
    }

    public static MaterialSymbol iconImagesearchRoller() {
        return symbols.get("imagesearch_roller");
    }

    public static MaterialSymbol iconImagesmode() {
        return symbols.get("imagesmode");
    }

    public static MaterialSymbol iconImmunology() {
        return symbols.get("immunology");
    }

    public static MaterialSymbol iconImportContacts() {
        return symbols.get("import_contacts");
    }

    public static MaterialSymbol iconImportExport() {
        return symbols.get("import_export");
    }

    public static MaterialSymbol iconImportantDevices() {
        return symbols.get("important_devices");
    }

    public static MaterialSymbol iconInHomeMode() {
        return symbols.get("in_home_mode");
    }

    public static MaterialSymbol iconInactiveOrder() {
        return symbols.get("inactive_order");
    }

    public static MaterialSymbol iconInbox() {
        return symbols.get("inbox");
    }

    public static MaterialSymbol iconInboxCustomize() {
        return symbols.get("inbox_customize");
    }

    public static MaterialSymbol iconInboxText() {
        return symbols.get("inbox_text");
    }

    public static MaterialSymbol iconInboxTextAsterisk() {
        return symbols.get("inbox_text_asterisk");
    }

    public static MaterialSymbol iconInboxTextPerson() {
        return symbols.get("inbox_text_person");
    }

    public static MaterialSymbol iconInboxTextShare() {
        return symbols.get("inbox_text_share");
    }

    public static MaterialSymbol iconIncompleteCircle() {
        return symbols.get("incomplete_circle");
    }

    public static MaterialSymbol iconIndeterminateCheckBox() {
        return symbols.get("indeterminate_check_box");
    }

    public static MaterialSymbol iconIndeterminateQuestionBox() {
        return symbols.get("indeterminate_question_box");
    }

    public static MaterialSymbol iconInfo() {
        return symbols.get("info");
    }

    public static MaterialSymbol iconInfoI() {
        return symbols.get("info_i");
    }

    public static MaterialSymbol iconInfrared() {
        return symbols.get("infrared");
    }

    public static MaterialSymbol iconInkEraser() {
        return symbols.get("ink_eraser");
    }

    public static MaterialSymbol iconInkEraserOff() {
        return symbols.get("ink_eraser_off");
    }

    public static MaterialSymbol iconInkHighlighter() {
        return symbols.get("ink_highlighter");
    }

    public static MaterialSymbol iconInkHighlighterMove() {
        return symbols.get("ink_highlighter_move");
    }

    public static MaterialSymbol iconInkMarker() {
        return symbols.get("ink_marker");
    }

    public static MaterialSymbol iconInkPen() {
        return symbols.get("ink_pen");
    }

    public static MaterialSymbol iconInkSelection() {
        return symbols.get("ink_selection");
    }

    public static MaterialSymbol iconInpatient() {
        return symbols.get("inpatient");
    }

    public static MaterialSymbol iconInput() {
        return symbols.get("input");
    }

    public static MaterialSymbol iconInputCircle() {
        return symbols.get("input_circle");
    }

    public static MaterialSymbol iconInsertChart() {
        return symbols.get("insert_chart");
    }

    public static MaterialSymbol iconInsertChartFilled() {
        return symbols.get("insert_chart_filled");
    }

    public static MaterialSymbol iconInsertChartOutlined() {
        return symbols.get("insert_chart_outlined");
    }

    public static MaterialSymbol iconInsertComment() {
        return symbols.get("insert_comment");
    }

    public static MaterialSymbol iconInsertDriveFile() {
        return symbols.get("insert_drive_file");
    }

    public static MaterialSymbol iconInsertEmoticon() {
        return symbols.get("insert_emoticon");
    }

    public static MaterialSymbol iconInsertInvitation() {
        return symbols.get("insert_invitation");
    }

    public static MaterialSymbol iconInsertLink() {
        return symbols.get("insert_link");
    }

    public static MaterialSymbol iconInsertPageBreak() {
        return symbols.get("insert_page_break");
    }

    public static MaterialSymbol iconInsertPhoto() {
        return symbols.get("insert_photo");
    }

    public static MaterialSymbol iconInsertText() {
        return symbols.get("insert_text");
    }

    public static MaterialSymbol iconInsights() {
        return symbols.get("insights");
    }

    public static MaterialSymbol iconInstallDesktop() {
        return symbols.get("install_desktop");
    }

    public static MaterialSymbol iconInstallMobile() {
        return symbols.get("install_mobile");
    }

    public static MaterialSymbol iconInstantMix() {
        return symbols.get("instant_mix");
    }

    public static MaterialSymbol iconIntegrationInstructions() {
        return symbols.get("integration_instructions");
    }

    public static MaterialSymbol iconInteractiveSpace() {
        return symbols.get("interactive_space");
    }

    public static MaterialSymbol iconInterests() {
        return symbols.get("interests");
    }

    public static MaterialSymbol iconInterpreterMode() {
        return symbols.get("interpreter_mode");
    }

    public static MaterialSymbol iconInventory() {
        return symbols.get("inventory");
    }

    public static MaterialSymbol iconInventory2() {
        return symbols.get("inventory_2");
    }

    public static MaterialSymbol iconInvertColors() {
        return symbols.get("invert_colors");
    }

    public static MaterialSymbol iconInvertColorsOff() {
        return symbols.get("invert_colors_off");
    }

    public static MaterialSymbol iconIos() {
        return symbols.get("ios");
    }

    public static MaterialSymbol iconIosShare() {
        return symbols.get("ios_share");
    }

    public static MaterialSymbol iconIron() {
        return symbols.get("iron");
    }

    public static MaterialSymbol iconIso() {
        return symbols.get("iso");
    }

    public static MaterialSymbol iconJamboardKiosk() {
        return symbols.get("jamboard_kiosk");
    }

    public static MaterialSymbol iconJapaneseCurry() {
        return symbols.get("japanese_curry");
    }

    public static MaterialSymbol iconJapaneseFlag() {
        return symbols.get("japanese_flag");
    }

    public static MaterialSymbol iconJavascript() {
        return symbols.get("javascript");
    }

    public static MaterialSymbol iconJoin() {
        return symbols.get("join");
    }

    public static MaterialSymbol iconJoinFull() {
        return symbols.get("join_full");
    }

    public static MaterialSymbol iconJoinInner() {
        return symbols.get("join_inner");
    }

    public static MaterialSymbol iconJoinLeft() {
        return symbols.get("join_left");
    }

    public static MaterialSymbol iconJoinRight() {
        return symbols.get("join_right");
    }

    public static MaterialSymbol iconJoystick() {
        return symbols.get("joystick");
    }

    public static MaterialSymbol iconJumpToElement() {
        return symbols.get("jump_to_element");
    }

    public static MaterialSymbol iconKanjiAlcohol() {
        return symbols.get("kanji_alcohol");
    }

    public static MaterialSymbol iconKayaking() {
        return symbols.get("kayaking");
    }

    public static MaterialSymbol iconKebabDining() {
        return symbols.get("kebab_dining");
    }

    public static MaterialSymbol iconKeep() {
        return symbols.get("keep");
    }

    public static MaterialSymbol iconKeepOff() {
        return symbols.get("keep_off");
    }

    public static MaterialSymbol iconKeepPin() {
        return symbols.get("keep_pin");
    }

    public static MaterialSymbol iconKeepPublic() {
        return symbols.get("keep_public");
    }

    public static MaterialSymbol iconKettle() {
        return symbols.get("kettle");
    }

    public static MaterialSymbol iconKey() {
        return symbols.get("key");
    }

    public static MaterialSymbol iconKeyOff() {
        return symbols.get("key_off");
    }

    public static MaterialSymbol iconKeyVertical() {
        return symbols.get("key_vertical");
    }

    public static MaterialSymbol iconKeyVisualizer() {
        return symbols.get("key_visualizer");
    }

    public static MaterialSymbol iconKeyboard() {
        return symbols.get("keyboard");
    }

    public static MaterialSymbol iconKeyboardAlt() {
        return symbols.get("keyboard_alt");
    }

    public static MaterialSymbol iconKeyboardArrowDown() {
        return symbols.get("keyboard_arrow_down");
    }

    public static MaterialSymbol iconKeyboardArrowLeft() {
        return symbols.get("keyboard_arrow_left");
    }

    public static MaterialSymbol iconKeyboardArrowRight() {
        return symbols.get("keyboard_arrow_right");
    }

    public static MaterialSymbol iconKeyboardArrowUp() {
        return symbols.get("keyboard_arrow_up");
    }

    public static MaterialSymbol iconKeyboardBackspace() {
        return symbols.get("keyboard_backspace");
    }

    public static MaterialSymbol iconKeyboardCapslock() {
        return symbols.get("keyboard_capslock");
    }

    public static MaterialSymbol iconKeyboardCapslockBadge() {
        return symbols.get("keyboard_capslock_badge");
    }

    public static MaterialSymbol iconKeyboardCommandKey() {
        return symbols.get("keyboard_command_key");
    }

    public static MaterialSymbol iconKeyboardControlKey() {
        return symbols.get("keyboard_control_key");
    }

    public static MaterialSymbol iconKeyboardDoubleArrowDown() {
        return symbols.get("keyboard_double_arrow_down");
    }

    public static MaterialSymbol iconKeyboardDoubleArrowLeft() {
        return symbols.get("keyboard_double_arrow_left");
    }

    public static MaterialSymbol iconKeyboardDoubleArrowRight() {
        return symbols.get("keyboard_double_arrow_right");
    }

    public static MaterialSymbol iconKeyboardDoubleArrowUp() {
        return symbols.get("keyboard_double_arrow_up");
    }

    public static MaterialSymbol iconKeyboardExternalInput() {
        return symbols.get("keyboard_external_input");
    }

    public static MaterialSymbol iconKeyboardFull() {
        return symbols.get("keyboard_full");
    }

    public static MaterialSymbol iconKeyboardHide() {
        return symbols.get("keyboard_hide");
    }

    public static MaterialSymbol iconKeyboardKeys() {
        return symbols.get("keyboard_keys");
    }

    public static MaterialSymbol iconKeyboardLock() {
        return symbols.get("keyboard_lock");
    }

    public static MaterialSymbol iconKeyboardLockOff() {
        return symbols.get("keyboard_lock_off");
    }

    public static MaterialSymbol iconKeyboardOff() {
        return symbols.get("keyboard_off");
    }

    public static MaterialSymbol iconKeyboardOnscreen() {
        return symbols.get("keyboard_onscreen");
    }

    public static MaterialSymbol iconKeyboardOptionKey() {
        return symbols.get("keyboard_option_key");
    }

    public static MaterialSymbol iconKeyboardPreviousLanguage() {
        return symbols.get("keyboard_previous_language");
    }

    public static MaterialSymbol iconKeyboardReturn() {
        return symbols.get("keyboard_return");
    }

    public static MaterialSymbol iconKeyboardTab() {
        return symbols.get("keyboard_tab");
    }

    public static MaterialSymbol iconKeyboardTabRtl() {
        return symbols.get("keyboard_tab_rtl");
    }

    public static MaterialSymbol iconKeyboardVoice() {
        return symbols.get("keyboard_voice");
    }

    public static MaterialSymbol iconKidStar() {
        return symbols.get("kid_star");
    }

    public static MaterialSymbol iconKingBed() {
        return symbols.get("king_bed");
    }

    public static MaterialSymbol iconKitchen() {
        return symbols.get("kitchen");
    }

    public static MaterialSymbol iconKitesurfing() {
        return symbols.get("kitesurfing");
    }

    public static MaterialSymbol iconLabPanel() {
        return symbols.get("lab_panel");
    }

    public static MaterialSymbol iconLabProfile() {
        return symbols.get("lab_profile");
    }

    public static MaterialSymbol iconLabResearch() {
        return symbols.get("lab_research");
    }

    public static MaterialSymbol iconLabel() {
        return symbols.get("label");
    }

    public static MaterialSymbol iconLabelImportant() {
        return symbols.get("label_important");
    }

    public static MaterialSymbol iconLabelImportantOutline() {
        return symbols.get("label_important_outline");
    }

    public static MaterialSymbol iconLabelOff() {
        return symbols.get("label_off");
    }

    public static MaterialSymbol iconLabelOutline() {
        return symbols.get("label_outline");
    }

    public static MaterialSymbol iconLabs() {
        return symbols.get("labs");
    }

    public static MaterialSymbol iconLan() {
        return symbols.get("lan");
    }

    public static MaterialSymbol iconLandscape() {
        return symbols.get("landscape");
    }

    public static MaterialSymbol iconLandscape2() {
        return symbols.get("landscape_2");
    }

    public static MaterialSymbol iconLandscape2Edit() {
        return symbols.get("landscape_2_edit");
    }

    public static MaterialSymbol iconLandscape2Off() {
        return symbols.get("landscape_2_off");
    }

    public static MaterialSymbol iconLandslide() {
        return symbols.get("landslide");
    }

    public static MaterialSymbol iconLanguage() {
        return symbols.get("language");
    }

    public static MaterialSymbol iconLanguageChineseArray() {
        return symbols.get("language_chinese_array");
    }

    public static MaterialSymbol iconLanguageChineseCangjie() {
        return symbols.get("language_chinese_cangjie");
    }

    public static MaterialSymbol iconLanguageChineseDayi() {
        return symbols.get("language_chinese_dayi");
    }

    public static MaterialSymbol iconLanguageChinesePinyin() {
        return symbols.get("language_chinese_pinyin");
    }

    public static MaterialSymbol iconLanguageChineseQuick() {
        return symbols.get("language_chinese_quick");
    }

    public static MaterialSymbol iconLanguageChineseWubi() {
        return symbols.get("language_chinese_wubi");
    }

    public static MaterialSymbol iconLanguageFrench() {
        return symbols.get("language_french");
    }

    public static MaterialSymbol iconLanguageGbEnglish() {
        return symbols.get("language_gb_english");
    }

    public static MaterialSymbol iconLanguageInternational() {
        return symbols.get("language_international");
    }

    public static MaterialSymbol iconLanguageJapaneseKana() {
        return symbols.get("language_japanese_kana");
    }

    public static MaterialSymbol iconLanguageKoreanLatin() {
        return symbols.get("language_korean_latin");
    }

    public static MaterialSymbol iconLanguagePinyin() {
        return symbols.get("language_pinyin");
    }

    public static MaterialSymbol iconLanguageSpanish() {
        return symbols.get("language_spanish");
    }

    public static MaterialSymbol iconLanguageUs() {
        return symbols.get("language_us");
    }

    public static MaterialSymbol iconLanguageUsColemak() {
        return symbols.get("language_us_colemak");
    }

    public static MaterialSymbol iconLanguageUsDvorak() {
        return symbols.get("language_us_dvorak");
    }

    public static MaterialSymbol iconLaps() {
        return symbols.get("laps");
    }

    public static MaterialSymbol iconLaptop() {
        return symbols.get("laptop");
    }

    public static MaterialSymbol iconLaptopCar() {
        return symbols.get("laptop_car");
    }

    public static MaterialSymbol iconLaptopChromebook() {
        return symbols.get("laptop_chromebook");
    }

    public static MaterialSymbol iconLaptopMac() {
        return symbols.get("laptop_mac");
    }

    public static MaterialSymbol iconLaptopWindows() {
        return symbols.get("laptop_windows");
    }

    public static MaterialSymbol iconLassoSelect() {
        return symbols.get("lasso_select");
    }

    public static MaterialSymbol iconLastPage() {
        return symbols.get("last_page");
    }

    public static MaterialSymbol iconLaunch() {
        return symbols.get("launch");
    }

    public static MaterialSymbol iconLaundry() {
        return symbols.get("laundry");
    }

    public static MaterialSymbol iconLayers() {
        return symbols.get("layers");
    }

    public static MaterialSymbol iconLayersClear() {
        return symbols.get("layers_clear");
    }

    public static MaterialSymbol iconLda() {
        return symbols.get("lda");
    }

    public static MaterialSymbol iconLeaderboard() {
        return symbols.get("leaderboard");
    }

    public static MaterialSymbol iconLeakAdd() {
        return symbols.get("leak_add");
    }

    public static MaterialSymbol iconLeakRemove() {
        return symbols.get("leak_remove");
    }

    public static MaterialSymbol iconLeftClick() {
        return symbols.get("left_click");
    }

    public static MaterialSymbol iconLeftPanelClose() {
        return symbols.get("left_panel_close");
    }

    public static MaterialSymbol iconLeftPanelOpen() {
        return symbols.get("left_panel_open");
    }

    public static MaterialSymbol iconLegendToggle() {
        return symbols.get("legend_toggle");
    }

    public static MaterialSymbol iconLens() {
        return symbols.get("lens");
    }

    public static MaterialSymbol iconLensBlur() {
        return symbols.get("lens_blur");
    }

    public static MaterialSymbol iconLetterSwitch() {
        return symbols.get("letter_switch");
    }

    public static MaterialSymbol iconLibraryAdd() {
        return symbols.get("library_add");
    }

    public static MaterialSymbol iconLibraryAddCheck() {
        return symbols.get("library_add_check");
    }

    public static MaterialSymbol iconLibraryBooks() {
        return symbols.get("library_books");
    }

    public static MaterialSymbol iconLibraryMusic() {
        return symbols.get("library_music");
    }

    public static MaterialSymbol iconLicense() {
        return symbols.get("license");
    }

    public static MaterialSymbol iconLiftToTalk() {
        return symbols.get("lift_to_talk");
    }

    public static MaterialSymbol iconLight() {
        return symbols.get("light");
    }

    public static MaterialSymbol iconLightGroup() {
        return symbols.get("light_group");
    }

    public static MaterialSymbol iconLightMode() {
        return symbols.get("light_mode");
    }

    public static MaterialSymbol iconLightOff() {
        return symbols.get("light_off");
    }

    public static MaterialSymbol iconLightbulb() {
        return symbols.get("lightbulb");
    }

    public static MaterialSymbol iconLightbulb2() {
        return symbols.get("lightbulb_2");
    }

    public static MaterialSymbol iconLightbulbCircle() {
        return symbols.get("lightbulb_circle");
    }

    public static MaterialSymbol iconLightbulbOutline() {
        return symbols.get("lightbulb_outline");
    }

    public static MaterialSymbol iconLightningStand() {
        return symbols.get("lightning_stand");
    }

    public static MaterialSymbol iconLineAxis() {
        return symbols.get("line_axis");
    }

    public static MaterialSymbol iconLineCurve() {
        return symbols.get("line_curve");
    }

    public static MaterialSymbol iconLineEnd() {
        return symbols.get("line_end");
    }

    public static MaterialSymbol iconLineEndArrow() {
        return symbols.get("line_end_arrow");
    }

    public static MaterialSymbol iconLineEndArrowNotch() {
        return symbols.get("line_end_arrow_notch");
    }

    public static MaterialSymbol iconLineEndCircle() {
        return symbols.get("line_end_circle");
    }

    public static MaterialSymbol iconLineEndDiamond() {
        return symbols.get("line_end_diamond");
    }

    public static MaterialSymbol iconLineEndSquare() {
        return symbols.get("line_end_square");
    }

    public static MaterialSymbol iconLineStart() {
        return symbols.get("line_start");
    }

    public static MaterialSymbol iconLineStartArrow() {
        return symbols.get("line_start_arrow");
    }

    public static MaterialSymbol iconLineStartArrowNotch() {
        return symbols.get("line_start_arrow_notch");
    }

    public static MaterialSymbol iconLineStartCircle() {
        return symbols.get("line_start_circle");
    }

    public static MaterialSymbol iconLineStartDiamond() {
        return symbols.get("line_start_diamond");
    }

    public static MaterialSymbol iconLineStartSquare() {
        return symbols.get("line_start_square");
    }

    public static MaterialSymbol iconLineStyle() {
        return symbols.get("line_style");
    }

    public static MaterialSymbol iconLineWeight() {
        return symbols.get("line_weight");
    }

    public static MaterialSymbol iconLinearScale() {
        return symbols.get("linear_scale");
    }

    public static MaterialSymbol iconLink() {
        return symbols.get("link");
    }

    public static MaterialSymbol iconLinkOff() {
        return symbols.get("link_off");
    }

    public static MaterialSymbol iconLinkedCamera() {
        return symbols.get("linked_camera");
    }

    public static MaterialSymbol iconLinkedServices() {
        return symbols.get("linked_services");
    }

    public static MaterialSymbol iconLiquor() {
        return symbols.get("liquor");
    }

    public static MaterialSymbol iconList() {
        return symbols.get("list");
    }

    public static MaterialSymbol iconListAlt() {
        return symbols.get("list_alt");
    }

    public static MaterialSymbol iconListAltAdd() {
        return symbols.get("list_alt_add");
    }

    public static MaterialSymbol iconListAltCheck() {
        return symbols.get("list_alt_check");
    }

    public static MaterialSymbol iconLists() {
        return symbols.get("lists");
    }

    public static MaterialSymbol iconLiveHelp() {
        return symbols.get("live_help");
    }

    public static MaterialSymbol iconLiveTv() {
        return symbols.get("live_tv");
    }

    public static MaterialSymbol iconLiving() {
        return symbols.get("living");
    }

    public static MaterialSymbol iconLocalActivity() {
        return symbols.get("local_activity");
    }

    public static MaterialSymbol iconLocalAirport() {
        return symbols.get("local_airport");
    }

    public static MaterialSymbol iconLocalAtm() {
        return symbols.get("local_atm");
    }

    public static MaterialSymbol iconLocalBar() {
        return symbols.get("local_bar");
    }

    public static MaterialSymbol iconLocalCafe() {
        return symbols.get("local_cafe");
    }

    public static MaterialSymbol iconLocalCarWash() {
        return symbols.get("local_car_wash");
    }

    public static MaterialSymbol iconLocalConvenienceStore() {
        return symbols.get("local_convenience_store");
    }

    public static MaterialSymbol iconLocalDining() {
        return symbols.get("local_dining");
    }

    public static MaterialSymbol iconLocalDrink() {
        return symbols.get("local_drink");
    }

    public static MaterialSymbol iconLocalFireDepartment() {
        return symbols.get("local_fire_department");
    }

    public static MaterialSymbol iconLocalFlorist() {
        return symbols.get("local_florist");
    }

    public static MaterialSymbol iconLocalGasStation() {
        return symbols.get("local_gas_station");
    }

    public static MaterialSymbol iconLocalGroceryStore() {
        return symbols.get("local_grocery_store");
    }

    public static MaterialSymbol iconLocalHospital() {
        return symbols.get("local_hospital");
    }

    public static MaterialSymbol iconLocalHotel() {
        return symbols.get("local_hotel");
    }

    public static MaterialSymbol iconLocalLaundryService() {
        return symbols.get("local_laundry_service");
    }

    public static MaterialSymbol iconLocalLibrary() {
        return symbols.get("local_library");
    }

    public static MaterialSymbol iconLocalMall() {
        return symbols.get("local_mall");
    }

    public static MaterialSymbol iconLocalMovies() {
        return symbols.get("local_movies");
    }

    public static MaterialSymbol iconLocalOffer() {
        return symbols.get("local_offer");
    }

    public static MaterialSymbol iconLocalParking() {
        return symbols.get("local_parking");
    }

    public static MaterialSymbol iconLocalPharmacy() {
        return symbols.get("local_pharmacy");
    }

    public static MaterialSymbol iconLocalPhone() {
        return symbols.get("local_phone");
    }

    public static MaterialSymbol iconLocalPizza() {
        return symbols.get("local_pizza");
    }

    public static MaterialSymbol iconLocalPlay() {
        return symbols.get("local_play");
    }

    public static MaterialSymbol iconLocalPolice() {
        return symbols.get("local_police");
    }

    public static MaterialSymbol iconLocalPostOffice() {
        return symbols.get("local_post_office");
    }

    public static MaterialSymbol iconLocalPrintshop() {
        return symbols.get("local_printshop");
    }

    public static MaterialSymbol iconLocalSee() {
        return symbols.get("local_see");
    }

    public static MaterialSymbol iconLocalShipping() {
        return symbols.get("local_shipping");
    }

    public static MaterialSymbol iconLocalTaxi() {
        return symbols.get("local_taxi");
    }

    public static MaterialSymbol iconLocationAutomation() {
        return symbols.get("location_automation");
    }

    public static MaterialSymbol iconLocationAway() {
        return symbols.get("location_away");
    }

    public static MaterialSymbol iconLocationChip() {
        return symbols.get("location_chip");
    }

    public static MaterialSymbol iconLocationCity() {
        return symbols.get("location_city");
    }

    public static MaterialSymbol iconLocationDisabled() {
        return symbols.get("location_disabled");
    }

    public static MaterialSymbol iconLocationHome() {
        return symbols.get("location_home");
    }

    public static MaterialSymbol iconLocationOff() {
        return symbols.get("location_off");
    }

    public static MaterialSymbol iconLocationOn() {
        return symbols.get("location_on");
    }

    public static MaterialSymbol iconLocationPin() {
        return symbols.get("location_pin");
    }

    public static MaterialSymbol iconLocationSearching() {
        return symbols.get("location_searching");
    }

    public static MaterialSymbol iconLocatorTag() {
        return symbols.get("locator_tag");
    }

    public static MaterialSymbol iconLock() {
        return symbols.get("lock");
    }

    public static MaterialSymbol iconLockClock() {
        return symbols.get("lock_clock");
    }

    public static MaterialSymbol iconLockOpen() {
        return symbols.get("lock_open");
    }

    public static MaterialSymbol iconLockOpenCircle() {
        return symbols.get("lock_open_circle");
    }

    public static MaterialSymbol iconLockOpenRight() {
        return symbols.get("lock_open_right");
    }

    public static MaterialSymbol iconLockOutline() {
        return symbols.get("lock_outline");
    }

    public static MaterialSymbol iconLockPerson() {
        return symbols.get("lock_person");
    }

    public static MaterialSymbol iconLockReset() {
        return symbols.get("lock_reset");
    }

    public static MaterialSymbol iconLogin() {
        return symbols.get("login");
    }

    public static MaterialSymbol iconLogoDev() {
        return symbols.get("logo_dev");
    }

    public static MaterialSymbol iconLogout() {
        return symbols.get("logout");
    }

    public static MaterialSymbol iconLooks() {
        return symbols.get("looks");
    }

    public static MaterialSymbol iconLooks3() {
        return symbols.get("looks_3");
    }

    public static MaterialSymbol iconLooks4() {
        return symbols.get("looks_4");
    }

    public static MaterialSymbol iconLooks5() {
        return symbols.get("looks_5");
    }

    public static MaterialSymbol iconLooks6() {
        return symbols.get("looks_6");
    }

    public static MaterialSymbol iconLooksOne() {
        return symbols.get("looks_one");
    }

    public static MaterialSymbol iconLooksTwo() {
        return symbols.get("looks_two");
    }

    public static MaterialSymbol iconLoop() {
        return symbols.get("loop");
    }

    public static MaterialSymbol iconLoupe() {
        return symbols.get("loupe");
    }

    public static MaterialSymbol iconLowDensity() {
        return symbols.get("low_density");
    }

    public static MaterialSymbol iconLowPriority() {
        return symbols.get("low_priority");
    }

    public static MaterialSymbol iconLowercase() {
        return symbols.get("lowercase");
    }

    public static MaterialSymbol iconLoyalty() {
        return symbols.get("loyalty");
    }

    public static MaterialSymbol iconLteMobiledata() {
        return symbols.get("lte_mobiledata");
    }

    public static MaterialSymbol iconLteMobiledataBadge() {
        return symbols.get("lte_mobiledata_badge");
    }

    public static MaterialSymbol iconLtePlusMobiledata() {
        return symbols.get("lte_plus_mobiledata");
    }

    public static MaterialSymbol iconLtePlusMobiledataBadge() {
        return symbols.get("lte_plus_mobiledata_badge");
    }

    public static MaterialSymbol iconLuggage() {
        return symbols.get("luggage");
    }

    public static MaterialSymbol iconLunchDining() {
        return symbols.get("lunch_dining");
    }

    public static MaterialSymbol iconLyrics() {
        return symbols.get("lyrics");
    }

    public static MaterialSymbol iconMacroAuto() {
        return symbols.get("macro_auto");
    }

    public static MaterialSymbol iconMacroOff() {
        return symbols.get("macro_off");
    }

    public static MaterialSymbol iconMagicButton() {
        return symbols.get("magic_button");
    }

    public static MaterialSymbol iconMagicExchange() {
        return symbols.get("magic_exchange");
    }

    public static MaterialSymbol iconMagicTether() {
        return symbols.get("magic_tether");
    }

    public static MaterialSymbol iconMagnificationLarge() {
        return symbols.get("magnification_large");
    }

    public static MaterialSymbol iconMagnificationSmall() {
        return symbols.get("magnification_small");
    }

    public static MaterialSymbol iconMagnifyDocked() {
        return symbols.get("magnify_docked");
    }

    public static MaterialSymbol iconMagnifyFullscreen() {
        return symbols.get("magnify_fullscreen");
    }

    public static MaterialSymbol iconMail() {
        return symbols.get("mail");
    }

    public static MaterialSymbol iconMailAsterisk() {
        return symbols.get("mail_asterisk");
    }

    public static MaterialSymbol iconMailLock() {
        return symbols.get("mail_lock");
    }

    public static MaterialSymbol iconMailOff() {
        return symbols.get("mail_off");
    }

    public static MaterialSymbol iconMailOutline() {
        return symbols.get("mail_outline");
    }

    public static MaterialSymbol iconMailShield() {
        return symbols.get("mail_shield");
    }

    public static MaterialSymbol iconMale() {
        return symbols.get("male");
    }

    public static MaterialSymbol iconMan() {
        return symbols.get("man");
    }

    public static MaterialSymbol iconMan2() {
        return symbols.get("man_2");
    }

    public static MaterialSymbol iconMan3() {
        return symbols.get("man_3");
    }

    public static MaterialSymbol iconMan4() {
        return symbols.get("man_4");
    }

    public static MaterialSymbol iconManageAccounts() {
        return symbols.get("manage_accounts");
    }

    public static MaterialSymbol iconManageHistory() {
        return symbols.get("manage_history");
    }

    public static MaterialSymbol iconManageSearch() {
        return symbols.get("manage_search");
    }

    public static MaterialSymbol iconManga() {
        return symbols.get("manga");
    }

    public static MaterialSymbol iconManufacturing() {
        return symbols.get("manufacturing");
    }

    public static MaterialSymbol iconMap() {
        return symbols.get("map");
    }

    public static MaterialSymbol iconMapPinHeart() {
        return symbols.get("map_pin_heart");
    }

    public static MaterialSymbol iconMapPinReview() {
        return symbols.get("map_pin_review");
    }

    public static MaterialSymbol iconMapSearch() {
        return symbols.get("map_search");
    }

    public static MaterialSymbol iconMapsHomeWork() {
        return symbols.get("maps_home_work");
    }

    public static MaterialSymbol iconMapsUgc() {
        return symbols.get("maps_ugc");
    }

    public static MaterialSymbol iconMargin() {
        return symbols.get("margin");
    }

    public static MaterialSymbol iconMarkAsUnread() {
        return symbols.get("mark_as_unread");
    }

    public static MaterialSymbol iconMarkChatRead() {
        return symbols.get("mark_chat_read");
    }

    public static MaterialSymbol iconMarkChatUnread() {
        return symbols.get("mark_chat_unread");
    }

    public static MaterialSymbol iconMarkEmailRead() {
        return symbols.get("mark_email_read");
    }

    public static MaterialSymbol iconMarkEmailUnread() {
        return symbols.get("mark_email_unread");
    }

    public static MaterialSymbol iconMarkUnreadChatAlt() {
        return symbols.get("mark_unread_chat_alt");
    }

    public static MaterialSymbol iconMarkdown() {
        return symbols.get("markdown");
    }

    public static MaterialSymbol iconMarkdownCopy() {
        return symbols.get("markdown_copy");
    }

    public static MaterialSymbol iconMarkdownPaste() {
        return symbols.get("markdown_paste");
    }

    public static MaterialSymbol iconMarkunread() {
        return symbols.get("markunread");
    }

    public static MaterialSymbol iconMarkunreadMailbox() {
        return symbols.get("markunread_mailbox");
    }

    public static MaterialSymbol iconMaskedTransitions() {
        return symbols.get("masked_transitions");
    }

    public static MaterialSymbol iconMaskedTransitionsAdd() {
        return symbols.get("masked_transitions_add");
    }

    public static MaterialSymbol iconMasks() {
        return symbols.get("masks");
    }

    public static MaterialSymbol iconMassage() {
        return symbols.get("massage");
    }

    public static MaterialSymbol iconMatchCase() {
        return symbols.get("match_case");
    }

    public static MaterialSymbol iconMatchCaseOff() {
        return symbols.get("match_case_off");
    }

    public static MaterialSymbol iconMatchWord() {
        return symbols.get("match_word");
    }

    public static MaterialSymbol iconMatter() {
        return symbols.get("matter");
    }

    public static MaterialSymbol iconMaximize() {
        return symbols.get("maximize");
    }

    public static MaterialSymbol iconMealDinner() {
        return symbols.get("meal_dinner");
    }

    public static MaterialSymbol iconMealLunch() {
        return symbols.get("meal_lunch");
    }

    public static MaterialSymbol iconMeasuringTape() {
        return symbols.get("measuring_tape");
    }

    public static MaterialSymbol iconMediaBluetoothOff() {
        return symbols.get("media_bluetooth_off");
    }

    public static MaterialSymbol iconMediaBluetoothOn() {
        return symbols.get("media_bluetooth_on");
    }

    public static MaterialSymbol iconMediaLink() {
        return symbols.get("media_link");
    }

    public static MaterialSymbol iconMediaOutput() {
        return symbols.get("media_output");
    }

    public static MaterialSymbol iconMediaOutputOff() {
        return symbols.get("media_output_off");
    }

    public static MaterialSymbol iconMediation() {
        return symbols.get("mediation");
    }

    public static MaterialSymbol iconMedicalInformation() {
        return symbols.get("medical_information");
    }

    public static MaterialSymbol iconMedicalMask() {
        return symbols.get("medical_mask");
    }

    public static MaterialSymbol iconMedicalServices() {
        return symbols.get("medical_services");
    }

    public static MaterialSymbol iconMedication() {
        return symbols.get("medication");
    }

    public static MaterialSymbol iconMedicationLiquid() {
        return symbols.get("medication_liquid");
    }

    public static MaterialSymbol iconMeetingRoom() {
        return symbols.get("meeting_room");
    }

    public static MaterialSymbol iconMemory() {
        return symbols.get("memory");
    }

    public static MaterialSymbol iconMemoryAlt() {
        return symbols.get("memory_alt");
    }

    public static MaterialSymbol iconMenstrualHealth() {
        return symbols.get("menstrual_health");
    }

    public static MaterialSymbol iconMenu() {
        return symbols.get("menu");
    }

    public static MaterialSymbol iconMenuBook() {
        return symbols.get("menu_book");
    }

    public static MaterialSymbol iconMenuBook2() {
        return symbols.get("menu_book_2");
    }

    public static MaterialSymbol iconMenuOpen() {
        return symbols.get("menu_open");
    }

    public static MaterialSymbol iconMerge() {
        return symbols.get("merge");
    }

    public static MaterialSymbol iconMergeType() {
        return symbols.get("merge_type");
    }

    public static MaterialSymbol iconMessage() {
        return symbols.get("message");
    }

    public static MaterialSymbol iconMetabolism() {
        return symbols.get("metabolism");
    }

    public static MaterialSymbol iconMetro() {
        return symbols.get("metro");
    }

    public static MaterialSymbol iconMfgNestYaleLock() {
        return symbols.get("mfg_nest_yale_lock");
    }

    public static MaterialSymbol iconMic() {
        return symbols.get("mic");
    }

    public static MaterialSymbol iconMicAlert() {
        return symbols.get("mic_alert");
    }

    public static MaterialSymbol iconMicDouble() {
        return symbols.get("mic_double");
    }

    public static MaterialSymbol iconMicExternalOff() {
        return symbols.get("mic_external_off");
    }

    public static MaterialSymbol iconMicExternalOn() {
        return symbols.get("mic_external_on");
    }

    public static MaterialSymbol iconMicNone() {
        return symbols.get("mic_none");
    }

    public static MaterialSymbol iconMicOff() {
        return symbols.get("mic_off");
    }

    public static MaterialSymbol iconMicrobiology() {
        return symbols.get("microbiology");
    }

    public static MaterialSymbol iconMicrowave() {
        return symbols.get("microwave");
    }

    public static MaterialSymbol iconMicrowaveGen() {
        return symbols.get("microwave_gen");
    }

    public static MaterialSymbol iconMilitaryTech() {
        return symbols.get("military_tech");
    }

    public static MaterialSymbol iconMimo() {
        return symbols.get("mimo");
    }

    public static MaterialSymbol iconMimoDisconnect() {
        return symbols.get("mimo_disconnect");
    }

    public static MaterialSymbol iconMindfulness() {
        return symbols.get("mindfulness");
    }

    public static MaterialSymbol iconMinimize() {
        return symbols.get("minimize");
    }

    public static MaterialSymbol iconMinorCrash() {
        return symbols.get("minor_crash");
    }

    public static MaterialSymbol iconMintmark() {
        return symbols.get("mintmark");
    }

    public static MaterialSymbol iconMissedVideoCall() {
        return symbols.get("missed_video_call");
    }

    public static MaterialSymbol iconMissedVideoCallFilled() {
        return symbols.get("missed_video_call_filled");
    }

    public static MaterialSymbol iconMissingController() {
        return symbols.get("missing_controller");
    }

    public static MaterialSymbol iconMist() {
        return symbols.get("mist");
    }

    public static MaterialSymbol iconMitre() {
        return symbols.get("mitre");
    }

    public static MaterialSymbol iconMixtureMed() {
        return symbols.get("mixture_med");
    }

    public static MaterialSymbol iconMms() {
        return symbols.get("mms");
    }

    public static MaterialSymbol iconMobile() {
        return symbols.get("mobile");
    }

    public static MaterialSymbol iconMobile2() {
        return symbols.get("mobile_2");
    }

    public static MaterialSymbol iconMobile3() {
        return symbols.get("mobile_3");
    }

    public static MaterialSymbol iconMobileAlert() {
        return symbols.get("mobile_alert");
    }

    public static MaterialSymbol iconMobileArrowDown() {
        return symbols.get("mobile_arrow_down");
    }

    public static MaterialSymbol iconMobileArrowRight() {
        return symbols.get("mobile_arrow_right");
    }

    public static MaterialSymbol iconMobileArrowUpRight() {
        return symbols.get("mobile_arrow_up_right");
    }

    public static MaterialSymbol iconMobileBlock() {
        return symbols.get("mobile_block");
    }

    public static MaterialSymbol iconMobileCamera() {
        return symbols.get("mobile_camera");
    }

    public static MaterialSymbol iconMobileCameraFront() {
        return symbols.get("mobile_camera_front");
    }

    public static MaterialSymbol iconMobileCameraRear() {
        return symbols.get("mobile_camera_rear");
    }

    public static MaterialSymbol iconMobileCancel() {
        return symbols.get("mobile_cancel");
    }

    public static MaterialSymbol iconMobileCast() {
        return symbols.get("mobile_cast");
    }

    public static MaterialSymbol iconMobileCharge() {
        return symbols.get("mobile_charge");
    }

    public static MaterialSymbol iconMobileChat() {
        return symbols.get("mobile_chat");
    }

    public static MaterialSymbol iconMobileCheck() {
        return symbols.get("mobile_check");
    }

    public static MaterialSymbol iconMobileCode() {
        return symbols.get("mobile_code");
    }

    public static MaterialSymbol iconMobileDots() {
        return symbols.get("mobile_dots");
    }

    public static MaterialSymbol iconMobileFriendly() {
        return symbols.get("mobile_friendly");
    }

    public static MaterialSymbol iconMobileGear() {
        return symbols.get("mobile_gear");
    }

    public static MaterialSymbol iconMobileHand() {
        return symbols.get("mobile_hand");
    }

    public static MaterialSymbol iconMobileHandLeft() {
        return symbols.get("mobile_hand_left");
    }

    public static MaterialSymbol iconMobileHandLeftOff() {
        return symbols.get("mobile_hand_left_off");
    }

    public static MaterialSymbol iconMobileHandOff() {
        return symbols.get("mobile_hand_off");
    }

    public static MaterialSymbol iconMobileInfo() {
        return symbols.get("mobile_info");
    }

    public static MaterialSymbol iconMobileLandscape() {
        return symbols.get("mobile_landscape");
    }

    public static MaterialSymbol iconMobileLayout() {
        return symbols.get("mobile_layout");
    }

    public static MaterialSymbol iconMobileLockLandscape() {
        return symbols.get("mobile_lock_landscape");
    }

    public static MaterialSymbol iconMobileLockPortrait() {
        return symbols.get("mobile_lock_portrait");
    }

    public static MaterialSymbol iconMobileLoupe() {
        return symbols.get("mobile_loupe");
    }

    public static MaterialSymbol iconMobileMenu() {
        return symbols.get("mobile_menu");
    }

    public static MaterialSymbol iconMobileOff() {
        return symbols.get("mobile_off");
    }

    public static MaterialSymbol iconMobileQuestion() {
        return symbols.get("mobile_question");
    }

    public static MaterialSymbol iconMobileRotate() {
        return symbols.get("mobile_rotate");
    }

    public static MaterialSymbol iconMobileRotateLock() {
        return symbols.get("mobile_rotate_lock");
    }

    public static MaterialSymbol iconMobileScreenShare() {
        return symbols.get("mobile_screen_share");
    }

    public static MaterialSymbol iconMobileScreensaver() {
        return symbols.get("mobile_screensaver");
    }

    public static MaterialSymbol iconMobileSensorHi() {
        return symbols.get("mobile_sensor_hi");
    }

    public static MaterialSymbol iconMobileSensorLo() {
        return symbols.get("mobile_sensor_lo");
    }

    public static MaterialSymbol iconMobileShare() {
        return symbols.get("mobile_share");
    }

    public static MaterialSymbol iconMobileShareStack() {
        return symbols.get("mobile_share_stack");
    }

    public static MaterialSymbol iconMobileSound() {
        return symbols.get("mobile_sound");
    }

    public static MaterialSymbol iconMobileSound2() {
        return symbols.get("mobile_sound_2");
    }

    public static MaterialSymbol iconMobileSoundOff() {
        return symbols.get("mobile_sound_off");
    }

    public static MaterialSymbol iconMobileSpeaker() {
        return symbols.get("mobile_speaker");
    }

    public static MaterialSymbol iconMobileText() {
        return symbols.get("mobile_text");
    }

    public static MaterialSymbol iconMobileText2() {
        return symbols.get("mobile_text_2");
    }

    public static MaterialSymbol iconMobileTheft() {
        return symbols.get("mobile_theft");
    }

    public static MaterialSymbol iconMobileTicket() {
        return symbols.get("mobile_ticket");
    }

    public static MaterialSymbol iconMobileVibrate() {
        return symbols.get("mobile_vibrate");
    }

    public static MaterialSymbol iconMobileWrench() {
        return symbols.get("mobile_wrench");
    }

    public static MaterialSymbol iconMobiledataOff() {
        return symbols.get("mobiledata_off");
    }

    public static MaterialSymbol iconMode() {
        return symbols.get("mode");
    }

    public static MaterialSymbol iconModeComment() {
        return symbols.get("mode_comment");
    }

    public static MaterialSymbol iconModeCool() {
        return symbols.get("mode_cool");
    }

    public static MaterialSymbol iconModeCoolOff() {
        return symbols.get("mode_cool_off");
    }

    public static MaterialSymbol iconModeDual() {
        return symbols.get("mode_dual");
    }

    public static MaterialSymbol iconModeEdit() {
        return symbols.get("mode_edit");
    }

    public static MaterialSymbol iconModeEditOutline() {
        return symbols.get("mode_edit_outline");
    }

    public static MaterialSymbol iconModeFan() {
        return symbols.get("mode_fan");
    }

    public static MaterialSymbol iconModeFanOff() {
        return symbols.get("mode_fan_off");
    }

    public static MaterialSymbol iconModeHeat() {
        return symbols.get("mode_heat");
    }

    public static MaterialSymbol iconModeHeatCool() {
        return symbols.get("mode_heat_cool");
    }

    public static MaterialSymbol iconModeHeatOff() {
        return symbols.get("mode_heat_off");
    }

    public static MaterialSymbol iconModeNight() {
        return symbols.get("mode_night");
    }

    public static MaterialSymbol iconModeOfTravel() {
        return symbols.get("mode_of_travel");
    }

    public static MaterialSymbol iconModeOffOn() {
        return symbols.get("mode_off_on");
    }

    public static MaterialSymbol iconModeStandby() {
        return symbols.get("mode_standby");
    }

    public static MaterialSymbol iconModelTraining() {
        return symbols.get("model_training");
    }

    public static MaterialSymbol iconModeling() {
        return symbols.get("modeling");
    }

    public static MaterialSymbol iconMonetizationOn() {
        return symbols.get("monetization_on");
    }

    public static MaterialSymbol iconMoney() {
        return symbols.get("money");
    }

    public static MaterialSymbol iconMoneyBag() {
        return symbols.get("money_bag");
    }

    public static MaterialSymbol iconMoneyOff() {
        return symbols.get("money_off");
    }

    public static MaterialSymbol iconMoneyOffCsred() {
        return symbols.get("money_off_csred");
    }

    public static MaterialSymbol iconMoneyRange() {
        return symbols.get("money_range");
    }

    public static MaterialSymbol iconMonitor() {
        return symbols.get("monitor");
    }

    public static MaterialSymbol iconMonitorHeart() {
        return symbols.get("monitor_heart");
    }

    public static MaterialSymbol iconMonitorWeight() {
        return symbols.get("monitor_weight");
    }

    public static MaterialSymbol iconMonitorWeightGain() {
        return symbols.get("monitor_weight_gain");
    }

    public static MaterialSymbol iconMonitorWeightLoss() {
        return symbols.get("monitor_weight_loss");
    }

    public static MaterialSymbol iconMonitoring() {
        return symbols.get("monitoring");
    }

    public static MaterialSymbol iconMonochromePhotos() {
        return symbols.get("monochrome_photos");
    }

    public static MaterialSymbol iconMonorail() {
        return symbols.get("monorail");
    }

    public static MaterialSymbol iconMood() {
        return symbols.get("mood");
    }

    public static MaterialSymbol iconMoodBad() {
        return symbols.get("mood_bad");
    }

    public static MaterialSymbol iconMoonStars() {
        return symbols.get("moon_stars");
    }

    public static MaterialSymbol iconMop() {
        return symbols.get("mop");
    }

    public static MaterialSymbol iconMoped() {
        return symbols.get("moped");
    }

    public static MaterialSymbol iconMopedPackage() {
        return symbols.get("moped_package");
    }

    public static MaterialSymbol iconMore() {
        return symbols.get("more");
    }

    public static MaterialSymbol iconMoreDown() {
        return symbols.get("more_down");
    }

    public static MaterialSymbol iconMoreHoriz() {
        return symbols.get("more_horiz");
    }

    public static MaterialSymbol iconMoreTime() {
        return symbols.get("more_time");
    }

    public static MaterialSymbol iconMoreUp() {
        return symbols.get("more_up");
    }

    public static MaterialSymbol iconMoreVert() {
        return symbols.get("more_vert");
    }

    public static MaterialSymbol iconMosque() {
        return symbols.get("mosque");
    }

    public static MaterialSymbol iconMotionBlur() {
        return symbols.get("motion_blur");
    }

    public static MaterialSymbol iconMotionMode() {
        return symbols.get("motion_mode");
    }

    public static MaterialSymbol iconMotionPhotosAuto() {
        return symbols.get("motion_photos_auto");
    }

    public static MaterialSymbol iconMotionPhotosOff() {
        return symbols.get("motion_photos_off");
    }

    public static MaterialSymbol iconMotionPhotosOn() {
        return symbols.get("motion_photos_on");
    }

    public static MaterialSymbol iconMotionPhotosPause() {
        return symbols.get("motion_photos_pause");
    }

    public static MaterialSymbol iconMotionPhotosPaused() {
        return symbols.get("motion_photos_paused");
    }

    public static MaterialSymbol iconMotionPlay() {
        return symbols.get("motion_play");
    }

    public static MaterialSymbol iconMotionSensorActive() {
        return symbols.get("motion_sensor_active");
    }

    public static MaterialSymbol iconMotionSensorAlert() {
        return symbols.get("motion_sensor_alert");
    }

    public static MaterialSymbol iconMotionSensorIdle() {
        return symbols.get("motion_sensor_idle");
    }

    public static MaterialSymbol iconMotionSensorUrgent() {
        return symbols.get("motion_sensor_urgent");
    }

    public static MaterialSymbol iconMotorcycle() {
        return symbols.get("motorcycle");
    }

    public static MaterialSymbol iconMountainFlag() {
        return symbols.get("mountain_flag");
    }

    public static MaterialSymbol iconMountainSteam() {
        return symbols.get("mountain_steam");
    }

    public static MaterialSymbol iconMouse() {
        return symbols.get("mouse");
    }

    public static MaterialSymbol iconMouseLock() {
        return symbols.get("mouse_lock");
    }

    public static MaterialSymbol iconMouseLockOff() {
        return symbols.get("mouse_lock_off");
    }

    public static MaterialSymbol iconMove() {
        return symbols.get("move");
    }

    public static MaterialSymbol iconMoveDown() {
        return symbols.get("move_down");
    }

    public static MaterialSymbol iconMoveGroup() {
        return symbols.get("move_group");
    }

    public static MaterialSymbol iconMoveItem() {
        return symbols.get("move_item");
    }

    public static MaterialSymbol iconMoveLocation() {
        return symbols.get("move_location");
    }

    public static MaterialSymbol iconMoveSelectionDown() {
        return symbols.get("move_selection_down");
    }

    public static MaterialSymbol iconMoveSelectionLeft() {
        return symbols.get("move_selection_left");
    }

    public static MaterialSymbol iconMoveSelectionRight() {
        return symbols.get("move_selection_right");
    }

    public static MaterialSymbol iconMoveSelectionUp() {
        return symbols.get("move_selection_up");
    }

    public static MaterialSymbol iconMoveToInbox() {
        return symbols.get("move_to_inbox");
    }

    public static MaterialSymbol iconMoveUp() {
        return symbols.get("move_up");
    }

    public static MaterialSymbol iconMovedLocation() {
        return symbols.get("moved_location");
    }

    public static MaterialSymbol iconMovie() {
        return symbols.get("movie");
    }

    public static MaterialSymbol iconMovieCreation() {
        return symbols.get("movie_creation");
    }

    public static MaterialSymbol iconMovieEdit() {
        return symbols.get("movie_edit");
    }

    public static MaterialSymbol iconMovieFilter() {
        return symbols.get("movie_filter");
    }

    public static MaterialSymbol iconMovieInfo() {
        return symbols.get("movie_info");
    }

    public static MaterialSymbol iconMovieOff() {
        return symbols.get("movie_off");
    }

    public static MaterialSymbol iconMovieSpeaker() {
        return symbols.get("movie_speaker");
    }

    public static MaterialSymbol iconMoving() {
        return symbols.get("moving");
    }

    public static MaterialSymbol iconMovingBeds() {
        return symbols.get("moving_beds");
    }

    public static MaterialSymbol iconMovingMinistry() {
        return symbols.get("moving_ministry");
    }

    public static MaterialSymbol iconMp() {
        return symbols.get("mp");
    }

    public static MaterialSymbol iconMulticooker() {
        return symbols.get("multicooker");
    }

    public static MaterialSymbol iconMultilineChart() {
        return symbols.get("multiline_chart");
    }

    public static MaterialSymbol iconMultimodalHandEye() {
        return symbols.get("multimodal_hand_eye");
    }

    public static MaterialSymbol iconMultipleAirports() {
        return symbols.get("multiple_airports");
    }

    public static MaterialSymbol iconMultipleStop() {
        return symbols.get("multiple_stop");
    }

    public static MaterialSymbol iconMuseum() {
        return symbols.get("museum");
    }

    public static MaterialSymbol iconMusicCast() {
        return symbols.get("music_cast");
    }

    public static MaterialSymbol iconMusicHistory() {
        return symbols.get("music_history");
    }

    public static MaterialSymbol iconMusicNote() {
        return symbols.get("music_note");
    }

    public static MaterialSymbol iconMusicNoteAdd() {
        return symbols.get("music_note_add");
    }

    public static MaterialSymbol iconMusicOff() {
        return symbols.get("music_off");
    }

    public static MaterialSymbol iconMusicVideo() {
        return symbols.get("music_video");
    }

    public static MaterialSymbol iconMyLocation() {
        return symbols.get("my_location");
    }

    public static MaterialSymbol iconMystery() {
        return symbols.get("mystery");
    }

    public static MaterialSymbol iconNat() {
        return symbols.get("nat");
    }

    public static MaterialSymbol iconNature() {
        return symbols.get("nature");
    }

    public static MaterialSymbol iconNaturePeople() {
        return symbols.get("nature_people");
    }

    public static MaterialSymbol iconNavigateBefore() {
        return symbols.get("navigate_before");
    }

    public static MaterialSymbol iconNavigateNext() {
        return symbols.get("navigate_next");
    }

    public static MaterialSymbol iconNavigation() {
        return symbols.get("navigation");
    }

    public static MaterialSymbol iconNearMe() {
        return symbols.get("near_me");
    }

    public static MaterialSymbol iconNearMeDisabled() {
        return symbols.get("near_me_disabled");
    }

    public static MaterialSymbol iconNearby() {
        return symbols.get("nearby");
    }

    public static MaterialSymbol iconNearbyError() {
        return symbols.get("nearby_error");
    }

    public static MaterialSymbol iconNearbyOff() {
        return symbols.get("nearby_off");
    }

    public static MaterialSymbol iconNephrology() {
        return symbols.get("nephrology");
    }

    public static MaterialSymbol iconNestAudio() {
        return symbols.get("nest_audio");
    }

    public static MaterialSymbol iconNestCamFloodlight() {
        return symbols.get("nest_cam_floodlight");
    }

    public static MaterialSymbol iconNestCamIndoor() {
        return symbols.get("nest_cam_indoor");
    }

    public static MaterialSymbol iconNestCamIq() {
        return symbols.get("nest_cam_iq");
    }

    public static MaterialSymbol iconNestCamIqOutdoor() {
        return symbols.get("nest_cam_iq_outdoor");
    }

    public static MaterialSymbol iconNestCamMagnetMount() {
        return symbols.get("nest_cam_magnet_mount");
    }

    public static MaterialSymbol iconNestCamOutdoor() {
        return symbols.get("nest_cam_outdoor");
    }

    public static MaterialSymbol iconNestCamStand() {
        return symbols.get("nest_cam_stand");
    }

    public static MaterialSymbol iconNestCamWallMount() {
        return symbols.get("nest_cam_wall_mount");
    }

    public static MaterialSymbol iconNestCamWiredStand() {
        return symbols.get("nest_cam_wired_stand");
    }

    public static MaterialSymbol iconNestClockFarsightAnalog() {
        return symbols.get("nest_clock_farsight_analog");
    }

    public static MaterialSymbol iconNestClockFarsightDigital() {
        return symbols.get("nest_clock_farsight_digital");
    }

    public static MaterialSymbol iconNestConnect() {
        return symbols.get("nest_connect");
    }

    public static MaterialSymbol iconNestDetect() {
        return symbols.get("nest_detect");
    }

    public static MaterialSymbol iconNestDisplay() {
        return symbols.get("nest_display");
    }

    public static MaterialSymbol iconNestDisplayMax() {
        return symbols.get("nest_display_max");
    }

    public static MaterialSymbol iconNestDoorbellVisitor() {
        return symbols.get("nest_doorbell_visitor");
    }

    public static MaterialSymbol iconNestEcoLeaf() {
        return symbols.get("nest_eco_leaf");
    }

    public static MaterialSymbol iconNestFarsightCool() {
        return symbols.get("nest_farsight_cool");
    }

    public static MaterialSymbol iconNestFarsightDual() {
        return symbols.get("nest_farsight_dual");
    }

    public static MaterialSymbol iconNestFarsightEco() {
        return symbols.get("nest_farsight_eco");
    }

    public static MaterialSymbol iconNestFarsightHeat() {
        return symbols.get("nest_farsight_heat");
    }

    public static MaterialSymbol iconNestFarsightSeasonal() {
        return symbols.get("nest_farsight_seasonal");
    }

    public static MaterialSymbol iconNestFarsightWeather() {
        return symbols.get("nest_farsight_weather");
    }

    public static MaterialSymbol iconNestFoundSavings() {
        return symbols.get("nest_found_savings");
    }

    public static MaterialSymbol iconNestGaleWifi() {
        return symbols.get("nest_gale_wifi");
    }

    public static MaterialSymbol iconNestHeatLinkE() {
        return symbols.get("nest_heat_link_e");
    }

    public static MaterialSymbol iconNestHeatLinkGen3() {
        return symbols.get("nest_heat_link_gen_3");
    }

    public static MaterialSymbol iconNestHelloDoorbell() {
        return symbols.get("nest_hello_doorbell");
    }

    public static MaterialSymbol iconNestLocatorTag() {
        return symbols.get("nest_locator_tag");
    }

    public static MaterialSymbol iconNestMini() {
        return symbols.get("nest_mini");
    }

    public static MaterialSymbol iconNestMultiRoom() {
        return symbols.get("nest_multi_room");
    }

    public static MaterialSymbol iconNestProtect() {
        return symbols.get("nest_protect");
    }

    public static MaterialSymbol iconNestRemote() {
        return symbols.get("nest_remote");
    }

    public static MaterialSymbol iconNestRemoteComfortSensor() {
        return symbols.get("nest_remote_comfort_sensor");
    }

    public static MaterialSymbol iconNestSecureAlarm() {
        return symbols.get("nest_secure_alarm");
    }

    public static MaterialSymbol iconNestSunblock() {
        return symbols.get("nest_sunblock");
    }

    public static MaterialSymbol iconNestTag() {
        return symbols.get("nest_tag");
    }

    public static MaterialSymbol iconNestThermostat() {
        return symbols.get("nest_thermostat");
    }

    public static MaterialSymbol iconNestThermostatEEu() {
        return symbols.get("nest_thermostat_e_eu");
    }

    public static MaterialSymbol iconNestThermostatGen3() {
        return symbols.get("nest_thermostat_gen_3");
    }

    public static MaterialSymbol iconNestThermostatSensor() {
        return symbols.get("nest_thermostat_sensor");
    }

    public static MaterialSymbol iconNestThermostatSensorEu() {
        return symbols.get("nest_thermostat_sensor_eu");
    }

    public static MaterialSymbol iconNestThermostatZirconiumEu() {
        return symbols.get("nest_thermostat_zirconium_eu");
    }

    public static MaterialSymbol iconNestTrueRadiant() {
        return symbols.get("nest_true_radiant");
    }

    public static MaterialSymbol iconNestWakeOnApproach() {
        return symbols.get("nest_wake_on_approach");
    }

    public static MaterialSymbol iconNestWakeOnPress() {
        return symbols.get("nest_wake_on_press");
    }

    public static MaterialSymbol iconNestWifiGale() {
        return symbols.get("nest_wifi_gale");
    }

    public static MaterialSymbol iconNestWifiMistral() {
        return symbols.get("nest_wifi_mistral");
    }

    public static MaterialSymbol iconNestWifiPoint() {
        return symbols.get("nest_wifi_point");
    }

    public static MaterialSymbol iconNestWifiPointVento() {
        return symbols.get("nest_wifi_point_vento");
    }

    public static MaterialSymbol iconNestWifiPro() {
        return symbols.get("nest_wifi_pro");
    }

    public static MaterialSymbol iconNestWifiPro2() {
        return symbols.get("nest_wifi_pro_2");
    }

    public static MaterialSymbol iconNestWifiRouter() {
        return symbols.get("nest_wifi_router");
    }

    public static MaterialSymbol iconNetworkCell() {
        return symbols.get("network_cell");
    }

    public static MaterialSymbol iconNetworkCheck() {
        return symbols.get("network_check");
    }

    public static MaterialSymbol iconNetworkIntelNode() {
        return symbols.get("network_intel_node");
    }

    public static MaterialSymbol iconNetworkIntelligence() {
        return symbols.get("network_intelligence");
    }

    public static MaterialSymbol iconNetworkIntelligenceHistory() {
        return symbols.get("network_intelligence_history");
    }

    public static MaterialSymbol iconNetworkIntelligenceUpdate() {
        return symbols.get("network_intelligence_update");
    }

    public static MaterialSymbol iconNetworkLocked() {
        return symbols.get("network_locked");
    }

    public static MaterialSymbol iconNetworkManage() {
        return symbols.get("network_manage");
    }

    public static MaterialSymbol iconNetworkNode() {
        return symbols.get("network_node");
    }

    public static MaterialSymbol iconNetworkPing() {
        return symbols.get("network_ping");
    }

    public static MaterialSymbol iconNetworkWifi() {
        return symbols.get("network_wifi");
    }

    public static MaterialSymbol iconNetworkWifi1Bar() {
        return symbols.get("network_wifi_1_bar");
    }

    public static MaterialSymbol iconNetworkWifi1BarLocked() {
        return symbols.get("network_wifi_1_bar_locked");
    }

    public static MaterialSymbol iconNetworkWifi2Bar() {
        return symbols.get("network_wifi_2_bar");
    }

    public static MaterialSymbol iconNetworkWifi2BarLocked() {
        return symbols.get("network_wifi_2_bar_locked");
    }

    public static MaterialSymbol iconNetworkWifi3Bar() {
        return symbols.get("network_wifi_3_bar");
    }

    public static MaterialSymbol iconNetworkWifi3BarLocked() {
        return symbols.get("network_wifi_3_bar_locked");
    }

    public static MaterialSymbol iconNetworkWifiLocked() {
        return symbols.get("network_wifi_locked");
    }

    public static MaterialSymbol iconNeurology() {
        return symbols.get("neurology");
    }

    public static MaterialSymbol iconNewLabel() {
        return symbols.get("new_label");
    }

    public static MaterialSymbol iconNewReleases() {
        return symbols.get("new_releases");
    }

    public static MaterialSymbol iconNewWindow() {
        return symbols.get("new_window");
    }

    public static MaterialSymbol iconNews() {
        return symbols.get("news");
    }

    public static MaterialSymbol iconNewsmode() {
        return symbols.get("newsmode");
    }

    public static MaterialSymbol iconNewspaper() {
        return symbols.get("newspaper");
    }

    public static MaterialSymbol iconNewsstand() {
        return symbols.get("newsstand");
    }

    public static MaterialSymbol iconNextPlan() {
        return symbols.get("next_plan");
    }

    public static MaterialSymbol iconNextWeek() {
        return symbols.get("next_week");
    }

    public static MaterialSymbol iconNfc() {
        return symbols.get("nfc");
    }

    public static MaterialSymbol iconNfcOff() {
        return symbols.get("nfc_off");
    }

    public static MaterialSymbol iconNightShelter() {
        return symbols.get("night_shelter");
    }

    public static MaterialSymbol iconNightSightAuto() {
        return symbols.get("night_sight_auto");
    }

    public static MaterialSymbol iconNightSightAutoOff() {
        return symbols.get("night_sight_auto_off");
    }

    public static MaterialSymbol iconNightSightMax() {
        return symbols.get("night_sight_max");
    }

    public static MaterialSymbol iconNightlife() {
        return symbols.get("nightlife");
    }

    public static MaterialSymbol iconNightlight() {
        return symbols.get("nightlight");
    }

    public static MaterialSymbol iconNightlightRound() {
        return symbols.get("nightlight_round");
    }

    public static MaterialSymbol iconNightsStay() {
        return symbols.get("nights_stay");
    }

    public static MaterialSymbol iconNoAccounts() {
        return symbols.get("no_accounts");
    }

    public static MaterialSymbol iconNoAdultContent() {
        return symbols.get("no_adult_content");
    }

    public static MaterialSymbol iconNoBackpack() {
        return symbols.get("no_backpack");
    }

    public static MaterialSymbol iconNoCrash() {
        return symbols.get("no_crash");
    }

    public static MaterialSymbol iconNoDrinks() {
        return symbols.get("no_drinks");
    }

    public static MaterialSymbol iconNoEncryption() {
        return symbols.get("no_encryption");
    }

    public static MaterialSymbol iconNoEncryptionGmailerrorred() {
        return symbols.get("no_encryption_gmailerrorred");
    }

    public static MaterialSymbol iconNoFlash() {
        return symbols.get("no_flash");
    }

    public static MaterialSymbol iconNoFood() {
        return symbols.get("no_food");
    }

    public static MaterialSymbol iconNoLuggage() {
        return symbols.get("no_luggage");
    }

    public static MaterialSymbol iconNoMeals() {
        return symbols.get("no_meals");
    }

    public static MaterialSymbol iconNoMeetingRoom() {
        return symbols.get("no_meeting_room");
    }

    public static MaterialSymbol iconNoPhotography() {
        return symbols.get("no_photography");
    }

    public static MaterialSymbol iconNoSim() {
        return symbols.get("no_sim");
    }

    public static MaterialSymbol iconNoSound() {
        return symbols.get("no_sound");
    }

    public static MaterialSymbol iconNoStroller() {
        return symbols.get("no_stroller");
    }

    public static MaterialSymbol iconNoTransfer() {
        return symbols.get("no_transfer");
    }

    public static MaterialSymbol iconNoiseAware() {
        return symbols.get("noise_aware");
    }

    public static MaterialSymbol iconNoiseControlOff() {
        return symbols.get("noise_control_off");
    }

    public static MaterialSymbol iconNoiseControlOn() {
        return symbols.get("noise_control_on");
    }

    public static MaterialSymbol iconNordicWalking() {
        return symbols.get("nordic_walking");
    }

    public static MaterialSymbol iconNorth() {
        return symbols.get("north");
    }

    public static MaterialSymbol iconNorthEast() {
        return symbols.get("north_east");
    }

    public static MaterialSymbol iconNorthWest() {
        return symbols.get("north_west");
    }

    public static MaterialSymbol iconNotAccessible() {
        return symbols.get("not_accessible");
    }

    public static MaterialSymbol iconNotAccessibleForward() {
        return symbols.get("not_accessible_forward");
    }

    public static MaterialSymbol iconNotInterested() {
        return symbols.get("not_interested");
    }

    public static MaterialSymbol iconNotListedLocation() {
        return symbols.get("not_listed_location");
    }

    public static MaterialSymbol iconNotStarted() {
        return symbols.get("not_started");
    }

    public static MaterialSymbol iconNote() {
        return symbols.get("note");
    }

    public static MaterialSymbol iconNoteAdd() {
        return symbols.get("note_add");
    }

    public static MaterialSymbol iconNoteAlt() {
        return symbols.get("note_alt");
    }

    public static MaterialSymbol iconNoteStack() {
        return symbols.get("note_stack");
    }

    public static MaterialSymbol iconNoteStackAdd() {
        return symbols.get("note_stack_add");
    }

    public static MaterialSymbol iconNotes() {
        return symbols.get("notes");
    }

    public static MaterialSymbol iconNotificationAdd() {
        return symbols.get("notification_add");
    }

    public static MaterialSymbol iconNotificationImportant() {
        return symbols.get("notification_important");
    }

    public static MaterialSymbol iconNotificationMultiple() {
        return symbols.get("notification_multiple");
    }

    public static MaterialSymbol iconNotificationSettings() {
        return symbols.get("notification_settings");
    }

    public static MaterialSymbol iconNotificationSound() {
        return symbols.get("notification_sound");
    }

    public static MaterialSymbol iconNotifications() {
        return symbols.get("notifications");
    }

    public static MaterialSymbol iconNotificationsActive() {
        return symbols.get("notifications_active");
    }

    public static MaterialSymbol iconNotificationsNone() {
        return symbols.get("notifications_none");
    }

    public static MaterialSymbol iconNotificationsOff() {
        return symbols.get("notifications_off");
    }

    public static MaterialSymbol iconNotificationsPaused() {
        return symbols.get("notifications_paused");
    }

    public static MaterialSymbol iconNotificationsUnread() {
        return symbols.get("notifications_unread");
    }

    public static MaterialSymbol iconNumbers() {
        return symbols.get("numbers");
    }

    public static MaterialSymbol iconNutrition() {
        return symbols.get("nutrition");
    }

    public static MaterialSymbol iconOds() {
        return symbols.get("ods");
    }

    public static MaterialSymbol iconOdt() {
        return symbols.get("odt");
    }

    public static MaterialSymbol iconOfflineBolt() {
        return symbols.get("offline_bolt");
    }

    public static MaterialSymbol iconOfflinePin() {
        return symbols.get("offline_pin");
    }

    public static MaterialSymbol iconOfflinePinOff() {
        return symbols.get("offline_pin_off");
    }

    public static MaterialSymbol iconOfflineShare() {
        return symbols.get("offline_share");
    }

    public static MaterialSymbol iconOilBarrel() {
        return symbols.get("oil_barrel");
    }

    public static MaterialSymbol iconOkonomiyaki() {
        return symbols.get("okonomiyaki");
    }

    public static MaterialSymbol iconOnDeviceTraining() {
        return symbols.get("on_device_training");
    }

    public static MaterialSymbol iconOnHubDevice() {
        return symbols.get("on_hub_device");
    }

    public static MaterialSymbol iconOncology() {
        return symbols.get("oncology");
    }

    public static MaterialSymbol iconOndemandVideo() {
        return symbols.get("ondemand_video");
    }

    public static MaterialSymbol iconOnlinePrediction() {
        return symbols.get("online_prediction");
    }

    public static MaterialSymbol iconOnsen() {
        return symbols.get("onsen");
    }

    public static MaterialSymbol iconOpacity() {
        return symbols.get("opacity");
    }

    public static MaterialSymbol iconOpenInBrowser() {
        return symbols.get("open_in_browser");
    }

    public static MaterialSymbol iconOpenInFull() {
        return symbols.get("open_in_full");
    }

    public static MaterialSymbol iconOpenInNew() {
        return symbols.get("open_in_new");
    }

    public static MaterialSymbol iconOpenInNewDown() {
        return symbols.get("open_in_new_down");
    }

    public static MaterialSymbol iconOpenInNewOff() {
        return symbols.get("open_in_new_off");
    }

    public static MaterialSymbol iconOpenInPhone() {
        return symbols.get("open_in_phone");
    }

    public static MaterialSymbol iconOpenJam() {
        return symbols.get("open_jam");
    }

    public static MaterialSymbol iconOpenRun() {
        return symbols.get("open_run");
    }

    public static MaterialSymbol iconOpenWith() {
        return symbols.get("open_with");
    }

    public static MaterialSymbol iconOphthalmology() {
        return symbols.get("ophthalmology");
    }

    public static MaterialSymbol iconOralDisease() {
        return symbols.get("oral_disease");
    }

    public static MaterialSymbol iconOrbit() {
        return symbols.get("orbit");
    }

    public static MaterialSymbol iconOrderApprove() {
        return symbols.get("order_approve");
    }

    public static MaterialSymbol iconOrderPlay() {
        return symbols.get("order_play");
    }

    public static MaterialSymbol iconOrders() {
        return symbols.get("orders");
    }

    public static MaterialSymbol iconOrthopedics() {
        return symbols.get("orthopedics");
    }

    public static MaterialSymbol iconOtherAdmission() {
        return symbols.get("other_admission");
    }

    public static MaterialSymbol iconOtherHouses() {
        return symbols.get("other_houses");
    }

    public static MaterialSymbol iconOutbound() {
        return symbols.get("outbound");
    }

    public static MaterialSymbol iconOutbox() {
        return symbols.get("outbox");
    }

    public static MaterialSymbol iconOutboxAlt() {
        return symbols.get("outbox_alt");
    }

    public static MaterialSymbol iconOutdoorGarden() {
        return symbols.get("outdoor_garden");
    }

    public static MaterialSymbol iconOutdoorGrill() {
        return symbols.get("outdoor_grill");
    }

    public static MaterialSymbol iconOutgoingMail() {
        return symbols.get("outgoing_mail");
    }

    public static MaterialSymbol iconOutlet() {
        return symbols.get("outlet");
    }

    public static MaterialSymbol iconOutlinedFlag() {
        return symbols.get("outlined_flag");
    }

    public static MaterialSymbol iconOutpatient() {
        return symbols.get("outpatient");
    }

    public static MaterialSymbol iconOutpatientMed() {
        return symbols.get("outpatient_med");
    }

    public static MaterialSymbol iconOutput() {
        return symbols.get("output");
    }

    public static MaterialSymbol iconOutputCircle() {
        return symbols.get("output_circle");
    }

    public static MaterialSymbol iconOven() {
        return symbols.get("oven");
    }

    public static MaterialSymbol iconOvenGen() {
        return symbols.get("oven_gen");
    }

    public static MaterialSymbol iconOverview() {
        return symbols.get("overview");
    }

    public static MaterialSymbol iconOverviewKey() {
        return symbols.get("overview_key");
    }

    public static MaterialSymbol iconOwl() {
        return symbols.get("owl");
    }

    public static MaterialSymbol iconOxygenSaturation() {
        return symbols.get("oxygen_saturation");
    }

    public static MaterialSymbol iconP2p() {
        return symbols.get("p2p");
    }

    public static MaterialSymbol iconPace() {
        return symbols.get("pace");
    }

    public static MaterialSymbol iconPacemaker() {
        return symbols.get("pacemaker");
    }

    public static MaterialSymbol iconPackage() {
        return symbols.get("package");
    }

    public static MaterialSymbol iconPackage2() {
        return symbols.get("package_2");
    }

    public static MaterialSymbol iconPadding() {
        return symbols.get("padding");
    }

    public static MaterialSymbol iconPadel() {
        return symbols.get("padel");
    }

    public static MaterialSymbol iconPageControl() {
        return symbols.get("page_control");
    }

    public static MaterialSymbol iconPageFooter() {
        return symbols.get("page_footer");
    }

    public static MaterialSymbol iconPageHeader() {
        return symbols.get("page_header");
    }

    public static MaterialSymbol iconPageInfo() {
        return symbols.get("page_info");
    }

    public static MaterialSymbol iconPageMenuIos() {
        return symbols.get("page_menu_ios");
    }

    public static MaterialSymbol iconPageless() {
        return symbols.get("pageless");
    }

    public static MaterialSymbol iconPages() {
        return symbols.get("pages");
    }

    public static MaterialSymbol iconPageview() {
        return symbols.get("pageview");
    }

    public static MaterialSymbol iconPaid() {
        return symbols.get("paid");
    }

    public static MaterialSymbol iconPalette() {
        return symbols.get("palette");
    }

    public static MaterialSymbol iconPallet() {
        return symbols.get("pallet");
    }

    public static MaterialSymbol iconPanTool() {
        return symbols.get("pan_tool");
    }

    public static MaterialSymbol iconPanToolAlt() {
        return symbols.get("pan_tool_alt");
    }

    public static MaterialSymbol iconPanZoom() {
        return symbols.get("pan_zoom");
    }

    public static MaterialSymbol iconPanorama() {
        return symbols.get("panorama");
    }

    public static MaterialSymbol iconPanoramaFishEye() {
        return symbols.get("panorama_fish_eye");
    }

    public static MaterialSymbol iconPanoramaHorizontal() {
        return symbols.get("panorama_horizontal");
    }

    public static MaterialSymbol iconPanoramaPhotosphere() {
        return symbols.get("panorama_photosphere");
    }

    public static MaterialSymbol iconPanoramaVertical() {
        return symbols.get("panorama_vertical");
    }

    public static MaterialSymbol iconPanoramaWideAngle() {
        return symbols.get("panorama_wide_angle");
    }

    public static MaterialSymbol iconParagliding() {
        return symbols.get("paragliding");
    }

    public static MaterialSymbol iconParentChildDining() {
        return symbols.get("parent_child_dining");
    }

    public static MaterialSymbol iconPark() {
        return symbols.get("park");
    }

    public static MaterialSymbol iconParkingMeter() {
        return symbols.get("parking_meter");
    }

    public static MaterialSymbol iconParkingSign() {
        return symbols.get("parking_sign");
    }

    public static MaterialSymbol iconParkingValet() {
        return symbols.get("parking_valet");
    }

    public static MaterialSymbol iconPartlyCloudyDay() {
        return symbols.get("partly_cloudy_day");
    }

    public static MaterialSymbol iconPartlyCloudyNight() {
        return symbols.get("partly_cloudy_night");
    }

    public static MaterialSymbol iconPartnerExchange() {
        return symbols.get("partner_exchange");
    }

    public static MaterialSymbol iconPartnerHeart() {
        return symbols.get("partner_heart");
    }

    public static MaterialSymbol iconPartnerReports() {
        return symbols.get("partner_reports");
    }

    public static MaterialSymbol iconPartyMode() {
        return symbols.get("party_mode");
    }

    public static MaterialSymbol iconPasskey() {
        return symbols.get("passkey");
    }

    public static MaterialSymbol iconPassword() {
        return symbols.get("password");
    }

    public static MaterialSymbol iconPassword2() {
        return symbols.get("password_2");
    }

    public static MaterialSymbol iconPassword2Off() {
        return symbols.get("password_2_off");
    }

    public static MaterialSymbol iconPatientList() {
        return symbols.get("patient_list");
    }

    public static MaterialSymbol iconPattern() {
        return symbols.get("pattern");
    }

    public static MaterialSymbol iconPause() {
        return symbols.get("pause");
    }

    public static MaterialSymbol iconPauseCircle() {
        return symbols.get("pause_circle");
    }

    public static MaterialSymbol iconPauseCircleFilled() {
        return symbols.get("pause_circle_filled");
    }

    public static MaterialSymbol iconPauseCircleOutline() {
        return symbols.get("pause_circle_outline");
    }

    public static MaterialSymbol iconPausePresentation() {
        return symbols.get("pause_presentation");
    }

    public static MaterialSymbol iconPayment() {
        return symbols.get("payment");
    }

    public static MaterialSymbol iconPaymentArrowDown() {
        return symbols.get("payment_arrow_down");
    }

    public static MaterialSymbol iconPaymentCard() {
        return symbols.get("payment_card");
    }

    public static MaterialSymbol iconPayments() {
        return symbols.get("payments");
    }

    public static MaterialSymbol iconPedalBike() {
        return symbols.get("pedal_bike");
    }

    public static MaterialSymbol iconPediatrics() {
        return symbols.get("pediatrics");
    }

    public static MaterialSymbol iconPenSize1() {
        return symbols.get("pen_size_1");
    }

    public static MaterialSymbol iconPenSize2() {
        return symbols.get("pen_size_2");
    }

    public static MaterialSymbol iconPenSize3() {
        return symbols.get("pen_size_3");
    }

    public static MaterialSymbol iconPenSize4() {
        return symbols.get("pen_size_4");
    }

    public static MaterialSymbol iconPenSize5() {
        return symbols.get("pen_size_5");
    }

    public static MaterialSymbol iconPending() {
        return symbols.get("pending");
    }

    public static MaterialSymbol iconPendingActions() {
        return symbols.get("pending_actions");
    }

    public static MaterialSymbol iconPentagon() {
        return symbols.get("pentagon");
    }

    public static MaterialSymbol iconPeople() {
        return symbols.get("people");
    }

    public static MaterialSymbol iconPeopleAlt() {
        return symbols.get("people_alt");
    }

    public static MaterialSymbol iconPeopleOutline() {
        return symbols.get("people_outline");
    }

    public static MaterialSymbol iconPercent() {
        return symbols.get("percent");
    }

    public static MaterialSymbol iconPercentDiscount() {
        return symbols.get("percent_discount");
    }

    public static MaterialSymbol iconPerformanceMax() {
        return symbols.get("performance_max");
    }

    public static MaterialSymbol iconPergola() {
        return symbols.get("pergola");
    }

    public static MaterialSymbol iconPermCameraMic() {
        return symbols.get("perm_camera_mic");
    }

    public static MaterialSymbol iconPermContactCalendar() {
        return symbols.get("perm_contact_calendar");
    }

    public static MaterialSymbol iconPermDataSetting() {
        return symbols.get("perm_data_setting");
    }

    public static MaterialSymbol iconPermDeviceInformation() {
        return symbols.get("perm_device_information");
    }

    public static MaterialSymbol iconPermIdentity() {
        return symbols.get("perm_identity");
    }

    public static MaterialSymbol iconPermMedia() {
        return symbols.get("perm_media");
    }

    public static MaterialSymbol iconPermPhoneMsg() {
        return symbols.get("perm_phone_msg");
    }

    public static MaterialSymbol iconPermScanWifi() {
        return symbols.get("perm_scan_wifi");
    }

    public static MaterialSymbol iconPerson() {
        return symbols.get("person");
    }

    public static MaterialSymbol iconPerson2() {
        return symbols.get("person_2");
    }

    public static MaterialSymbol iconPerson3() {
        return symbols.get("person_3");
    }

    public static MaterialSymbol iconPerson4() {
        return symbols.get("person_4");
    }

    public static MaterialSymbol iconPersonAdd() {
        return symbols.get("person_add");
    }

    public static MaterialSymbol iconPersonAddAlt() {
        return symbols.get("person_add_alt");
    }

    public static MaterialSymbol iconPersonAddDisabled() {
        return symbols.get("person_add_disabled");
    }

    public static MaterialSymbol iconPersonAlert() {
        return symbols.get("person_alert");
    }

    public static MaterialSymbol iconPersonApron() {
        return symbols.get("person_apron");
    }

    public static MaterialSymbol iconPersonBook() {
        return symbols.get("person_book");
    }

    public static MaterialSymbol iconPersonCancel() {
        return symbols.get("person_cancel");
    }

    public static MaterialSymbol iconPersonCelebrate() {
        return symbols.get("person_celebrate");
    }

    public static MaterialSymbol iconPersonCheck() {
        return symbols.get("person_check");
    }

    public static MaterialSymbol iconPersonEdit() {
        return symbols.get("person_edit");
    }

    public static MaterialSymbol iconPersonFilled() {
        return symbols.get("person_filled");
    }

    public static MaterialSymbol iconPersonHeart() {
        return symbols.get("person_heart");
    }

    public static MaterialSymbol iconPersonOff() {
        return symbols.get("person_off");
    }

    public static MaterialSymbol iconPersonOutline() {
        return symbols.get("person_outline");
    }

    public static MaterialSymbol iconPersonPin() {
        return symbols.get("person_pin");
    }

    public static MaterialSymbol iconPersonPinCircle() {
        return symbols.get("person_pin_circle");
    }

    public static MaterialSymbol iconPersonPlay() {
        return symbols.get("person_play");
    }

    public static MaterialSymbol iconPersonRaisedHand() {
        return symbols.get("person_raised_hand");
    }

    public static MaterialSymbol iconPersonRemove() {
        return symbols.get("person_remove");
    }

    public static MaterialSymbol iconPersonSearch() {
        return symbols.get("person_search");
    }

    public static MaterialSymbol iconPersonShield() {
        return symbols.get("person_shield");
    }

    public static MaterialSymbol iconPersonalBag() {
        return symbols.get("personal_bag");
    }

    public static MaterialSymbol iconPersonalBagOff() {
        return symbols.get("personal_bag_off");
    }

    public static MaterialSymbol iconPersonalBagQuestion() {
        return symbols.get("personal_bag_question");
    }

    public static MaterialSymbol iconPersonalInjury() {
        return symbols.get("personal_injury");
    }

    public static MaterialSymbol iconPersonalPlaces() {
        return symbols.get("personal_places");
    }

    public static MaterialSymbol iconPersonalVideo() {
        return symbols.get("personal_video");
    }

    public static MaterialSymbol iconPestControl() {
        return symbols.get("pest_control");
    }

    public static MaterialSymbol iconPestControlRodent() {
        return symbols.get("pest_control_rodent");
    }

    public static MaterialSymbol iconPetSupplies() {
        return symbols.get("pet_supplies");
    }

    public static MaterialSymbol iconPets() {
        return symbols.get("pets");
    }

    public static MaterialSymbol iconPhishing() {
        return symbols.get("phishing");
    }

    public static MaterialSymbol iconPhone() {
        return symbols.get("phone");
    }

    public static MaterialSymbol iconPhoneAlt() {
        return symbols.get("phone_alt");
    }

    public static MaterialSymbol iconPhoneAndroid() {
        return symbols.get("phone_android");
    }

    public static MaterialSymbol iconPhoneBluetoothSpeaker() {
        return symbols.get("phone_bluetooth_speaker");
    }

    public static MaterialSymbol iconPhoneCallback() {
        return symbols.get("phone_callback");
    }

    public static MaterialSymbol iconPhoneDisabled() {
        return symbols.get("phone_disabled");
    }

    public static MaterialSymbol iconPhoneEnabled() {
        return symbols.get("phone_enabled");
    }

    public static MaterialSymbol iconPhoneForwarded() {
        return symbols.get("phone_forwarded");
    }

    public static MaterialSymbol iconPhoneInTalk() {
        return symbols.get("phone_in_talk");
    }

    public static MaterialSymbol iconPhoneIphone() {
        return symbols.get("phone_iphone");
    }

    public static MaterialSymbol iconPhoneLocked() {
        return symbols.get("phone_locked");
    }

    public static MaterialSymbol iconPhoneMissed() {
        return symbols.get("phone_missed");
    }

    public static MaterialSymbol iconPhonePaused() {
        return symbols.get("phone_paused");
    }

    public static MaterialSymbol iconPhonelink() {
        return symbols.get("phonelink");
    }

    public static MaterialSymbol iconPhonelinkErase() {
        return symbols.get("phonelink_erase");
    }

    public static MaterialSymbol iconPhonelinkLock() {
        return symbols.get("phonelink_lock");
    }

    public static MaterialSymbol iconPhonelinkOff() {
        return symbols.get("phonelink_off");
    }

    public static MaterialSymbol iconPhonelinkRing() {
        return symbols.get("phonelink_ring");
    }

    public static MaterialSymbol iconPhonelinkRingOff() {
        return symbols.get("phonelink_ring_off");
    }

    public static MaterialSymbol iconPhonelinkSetup() {
        return symbols.get("phonelink_setup");
    }

    public static MaterialSymbol iconPhoto() {
        return symbols.get("photo");
    }

    public static MaterialSymbol iconPhotoAlbum() {
        return symbols.get("photo_album");
    }

    public static MaterialSymbol iconPhotoAutoMerge() {
        return symbols.get("photo_auto_merge");
    }

    public static MaterialSymbol iconPhotoCamera() {
        return symbols.get("photo_camera");
    }

    public static MaterialSymbol iconPhotoCameraBack() {
        return symbols.get("photo_camera_back");
    }

    public static MaterialSymbol iconPhotoCameraFront() {
        return symbols.get("photo_camera_front");
    }

    public static MaterialSymbol iconPhotoFilter() {
        return symbols.get("photo_filter");
    }

    public static MaterialSymbol iconPhotoFrame() {
        return symbols.get("photo_frame");
    }

    public static MaterialSymbol iconPhotoLibrary() {
        return symbols.get("photo_library");
    }

    public static MaterialSymbol iconPhotoPrints() {
        return symbols.get("photo_prints");
    }

    public static MaterialSymbol iconPhotoSizeSelectActual() {
        return symbols.get("photo_size_select_actual");
    }

    public static MaterialSymbol iconPhotoSizeSelectLarge() {
        return symbols.get("photo_size_select_large");
    }

    public static MaterialSymbol iconPhotoSizeSelectSmall() {
        return symbols.get("photo_size_select_small");
    }

    public static MaterialSymbol iconPhp() {
        return symbols.get("php");
    }

    public static MaterialSymbol iconPhysicalTherapy() {
        return symbols.get("physical_therapy");
    }

    public static MaterialSymbol iconPiano() {
        return symbols.get("piano");
    }

    public static MaterialSymbol iconPianoOff() {
        return symbols.get("piano_off");
    }

    public static MaterialSymbol iconPickleball() {
        return symbols.get("pickleball");
    }

    public static MaterialSymbol iconPictureAsPdf() {
        return symbols.get("picture_as_pdf");
    }

    public static MaterialSymbol iconPictureInPicture() {
        return symbols.get("picture_in_picture");
    }

    public static MaterialSymbol iconPictureInPictureAlt() {
        return symbols.get("picture_in_picture_alt");
    }

    public static MaterialSymbol iconPictureInPictureCenter() {
        return symbols.get("picture_in_picture_center");
    }

    public static MaterialSymbol iconPictureInPictureLarge() {
        return symbols.get("picture_in_picture_large");
    }

    public static MaterialSymbol iconPictureInPictureMedium() {
        return symbols.get("picture_in_picture_medium");
    }

    public static MaterialSymbol iconPictureInPictureMobile() {
        return symbols.get("picture_in_picture_mobile");
    }

    public static MaterialSymbol iconPictureInPictureOff() {
        return symbols.get("picture_in_picture_off");
    }

    public static MaterialSymbol iconPictureInPictureSmall() {
        return symbols.get("picture_in_picture_small");
    }

    public static MaterialSymbol iconPieChart() {
        return symbols.get("pie_chart");
    }

    public static MaterialSymbol iconPieChartFilled() {
        return symbols.get("pie_chart_filled");
    }

    public static MaterialSymbol iconPieChartOutline() {
        return symbols.get("pie_chart_outline");
    }

    public static MaterialSymbol iconPieChartOutlined() {
        return symbols.get("pie_chart_outlined");
    }

    public static MaterialSymbol iconPill() {
        return symbols.get("pill");
    }

    public static MaterialSymbol iconPillOff() {
        return symbols.get("pill_off");
    }

    public static MaterialSymbol iconPin() {
        return symbols.get("pin");
    }

    public static MaterialSymbol iconPinDrop() {
        return symbols.get("pin_drop");
    }

    public static MaterialSymbol iconPinEnd() {
        return symbols.get("pin_end");
    }

    public static MaterialSymbol iconPinInvoke() {
        return symbols.get("pin_invoke");
    }

    public static MaterialSymbol iconPinboard() {
        return symbols.get("pinboard");
    }

    public static MaterialSymbol iconPinboardUnread() {
        return symbols.get("pinboard_unread");
    }

    public static MaterialSymbol iconPinch() {
        return symbols.get("pinch");
    }

    public static MaterialSymbol iconPinchZoomIn() {
        return symbols.get("pinch_zoom_in");
    }

    public static MaterialSymbol iconPinchZoomOut() {
        return symbols.get("pinch_zoom_out");
    }

    public static MaterialSymbol iconPip() {
        return symbols.get("pip");
    }

    public static MaterialSymbol iconPipExit() {
        return symbols.get("pip_exit");
    }

    public static MaterialSymbol iconPivotTableChart() {
        return symbols.get("pivot_table_chart");
    }

    public static MaterialSymbol iconPlace() {
        return symbols.get("place");
    }

    public static MaterialSymbol iconPlaceItem() {
        return symbols.get("place_item");
    }

    public static MaterialSymbol iconPlagiarism() {
        return symbols.get("plagiarism");
    }

    public static MaterialSymbol iconPlaneContrails() {
        return symbols.get("plane_contrails");
    }

    public static MaterialSymbol iconPlanet() {
        return symbols.get("planet");
    }

    public static MaterialSymbol iconPlannerBannerAdPt() {
        return symbols.get("planner_banner_ad_pt");
    }

    public static MaterialSymbol iconPlannerReview() {
        return symbols.get("planner_review");
    }

    public static MaterialSymbol iconPlayArrow() {
        return symbols.get("play_arrow");
    }

    public static MaterialSymbol iconPlayCircle() {
        return symbols.get("play_circle");
    }

    public static MaterialSymbol iconPlayDisabled() {
        return symbols.get("play_disabled");
    }

    public static MaterialSymbol iconPlayForWork() {
        return symbols.get("play_for_work");
    }

    public static MaterialSymbol iconPlayLesson() {
        return symbols.get("play_lesson");
    }

    public static MaterialSymbol iconPlayMusic() {
        return symbols.get("play_music");
    }

    public static MaterialSymbol iconPlayPause() {
        return symbols.get("play_pause");
    }

    public static MaterialSymbol iconPlayShapes() {
        return symbols.get("play_shapes");
    }

    public static MaterialSymbol iconPlayground() {
        return symbols.get("playground");
    }

    public static MaterialSymbol iconPlayground2() {
        return symbols.get("playground_2");
    }

    public static MaterialSymbol iconPlayingCards() {
        return symbols.get("playing_cards");
    }

    public static MaterialSymbol iconPlaylistAdd() {
        return symbols.get("playlist_add");
    }

    public static MaterialSymbol iconPlaylistAddCheck() {
        return symbols.get("playlist_add_check");
    }

    public static MaterialSymbol iconPlaylistAddCheckCircle() {
        return symbols.get("playlist_add_check_circle");
    }

    public static MaterialSymbol iconPlaylistAddCircle() {
        return symbols.get("playlist_add_circle");
    }

    public static MaterialSymbol iconPlaylistPlay() {
        return symbols.get("playlist_play");
    }

    public static MaterialSymbol iconPlaylistRemove() {
        return symbols.get("playlist_remove");
    }

    public static MaterialSymbol iconPlugConnect() {
        return symbols.get("plug_connect");
    }

    public static MaterialSymbol iconPlumbing() {
        return symbols.get("plumbing");
    }

    public static MaterialSymbol iconPlusOne() {
        return symbols.get("plus_one");
    }

    public static MaterialSymbol iconPodcasts() {
        return symbols.get("podcasts");
    }

    public static MaterialSymbol iconPodiatry() {
        return symbols.get("podiatry");
    }

    public static MaterialSymbol iconPodium() {
        return symbols.get("podium");
    }

    public static MaterialSymbol iconPointOfSale() {
        return symbols.get("point_of_sale");
    }

    public static MaterialSymbol iconPointScan() {
        return symbols.get("point_scan");
    }

    public static MaterialSymbol iconPokerChip() {
        return symbols.get("poker_chip");
    }

    public static MaterialSymbol iconPolicy() {
        return symbols.get("policy");
    }

    public static MaterialSymbol iconPolicyAlert() {
        return symbols.get("policy_alert");
    }

    public static MaterialSymbol iconPoll() {
        return symbols.get("poll");
    }

    public static MaterialSymbol iconPolyline() {
        return symbols.get("polyline");
    }

    public static MaterialSymbol iconPolymer() {
        return symbols.get("polymer");
    }

    public static MaterialSymbol iconPool() {
        return symbols.get("pool");
    }

    public static MaterialSymbol iconPortableWifiOff() {
        return symbols.get("portable_wifi_off");
    }

    public static MaterialSymbol iconPortrait() {
        return symbols.get("portrait");
    }

    public static MaterialSymbol iconPositionBottomLeft() {
        return symbols.get("position_bottom_left");
    }

    public static MaterialSymbol iconPositionBottomRight() {
        return symbols.get("position_bottom_right");
    }

    public static MaterialSymbol iconPositionTopRight() {
        return symbols.get("position_top_right");
    }

    public static MaterialSymbol iconPost() {
        return symbols.get("post");
    }

    public static MaterialSymbol iconPostAdd() {
        return symbols.get("post_add");
    }

    public static MaterialSymbol iconPottedPlant() {
        return symbols.get("potted_plant");
    }

    public static MaterialSymbol iconPower() {
        return symbols.get("power");
    }

    public static MaterialSymbol iconPowerInput() {
        return symbols.get("power_input");
    }

    public static MaterialSymbol iconPowerOff() {
        return symbols.get("power_off");
    }

    public static MaterialSymbol iconPowerRounded() {
        return symbols.get("power_rounded");
    }

    public static MaterialSymbol iconPowerSettingsCircle() {
        return symbols.get("power_settings_circle");
    }

    public static MaterialSymbol iconPowerSettingsNew() {
        return symbols.get("power_settings_new");
    }

    public static MaterialSymbol iconPrayerTimes() {
        return symbols.get("prayer_times");
    }

    public static MaterialSymbol iconPrecisionManufacturing() {
        return symbols.get("precision_manufacturing");
    }

    public static MaterialSymbol iconPregnancy() {
        return symbols.get("pregnancy");
    }

    public static MaterialSymbol iconPregnantWoman() {
        return symbols.get("pregnant_woman");
    }

    public static MaterialSymbol iconPreliminary() {
        return symbols.get("preliminary");
    }

    public static MaterialSymbol iconPrescriptions() {
        return symbols.get("prescriptions");
    }

    public static MaterialSymbol iconPresentToAll() {
        return symbols.get("present_to_all");
    }

    public static MaterialSymbol iconPreview() {
        return symbols.get("preview");
    }

    public static MaterialSymbol iconPreviewOff() {
        return symbols.get("preview_off");
    }

    public static MaterialSymbol iconPriceChange() {
        return symbols.get("price_change");
    }

    public static MaterialSymbol iconPriceCheck() {
        return symbols.get("price_check");
    }

    public static MaterialSymbol iconPrint() {
        return symbols.get("print");
    }

    public static MaterialSymbol iconPrintAdd() {
        return symbols.get("print_add");
    }

    public static MaterialSymbol iconPrintConnect() {
        return symbols.get("print_connect");
    }

    public static MaterialSymbol iconPrintDisabled() {
        return symbols.get("print_disabled");
    }

    public static MaterialSymbol iconPrintError() {
        return symbols.get("print_error");
    }

    public static MaterialSymbol iconPrintLock() {
        return symbols.get("print_lock");
    }

    public static MaterialSymbol iconPriority() {
        return symbols.get("priority");
    }

    public static MaterialSymbol iconPriorityHigh() {
        return symbols.get("priority_high");
    }

    public static MaterialSymbol iconPrivacy() {
        return symbols.get("privacy");
    }

    public static MaterialSymbol iconPrivacyTip() {
        return symbols.get("privacy_tip");
    }

    public static MaterialSymbol iconPrivateConnectivity() {
        return symbols.get("private_connectivity");
    }

    public static MaterialSymbol iconProblem() {
        return symbols.get("problem");
    }

    public static MaterialSymbol iconProcedure() {
        return symbols.get("procedure");
    }

    public static MaterialSymbol iconProcessChart() {
        return symbols.get("process_chart");
    }

    public static MaterialSymbol iconProductionQuantityLimits() {
        return symbols.get("production_quantity_limits");
    }

    public static MaterialSymbol iconProductivity() {
        return symbols.get("productivity");
    }

    public static MaterialSymbol iconProgressActivity() {
        return symbols.get("progress_activity");
    }

    public static MaterialSymbol iconPromptSuggestion() {
        return symbols.get("prompt_suggestion");
    }

    public static MaterialSymbol iconPropane() {
        return symbols.get("propane");
    }

    public static MaterialSymbol iconPropaneTank() {
        return symbols.get("propane_tank");
    }

    public static MaterialSymbol iconPsychiatry() {
        return symbols.get("psychiatry");
    }

    public static MaterialSymbol iconPsychology() {
        return symbols.get("psychology");
    }

    public static MaterialSymbol iconPsychologyAlt() {
        return symbols.get("psychology_alt");
    }

    public static MaterialSymbol iconPublic() {
        return symbols.get("public");
    }

    public static MaterialSymbol iconPublicOff() {
        return symbols.get("public_off");
    }

    public static MaterialSymbol iconPublish() {
        return symbols.get("publish");
    }

    public static MaterialSymbol iconPublishedWithChanges() {
        return symbols.get("published_with_changes");
    }

    public static MaterialSymbol iconPulmonology() {
        return symbols.get("pulmonology");
    }

    public static MaterialSymbol iconPulseAlert() {
        return symbols.get("pulse_alert");
    }

    public static MaterialSymbol iconPunchClock() {
        return symbols.get("punch_clock");
    }

    public static MaterialSymbol iconPushPin() {
        return symbols.get("push_pin");
    }

    public static MaterialSymbol iconQrCode() {
        return symbols.get("qr_code");
    }

    public static MaterialSymbol iconQrCode2() {
        return symbols.get("qr_code_2");
    }

    public static MaterialSymbol iconQrCode2Add() {
        return symbols.get("qr_code_2_add");
    }

    public static MaterialSymbol iconQrCodeScanner() {
        return symbols.get("qr_code_scanner");
    }

    public static MaterialSymbol iconQueryBuilder() {
        return symbols.get("query_builder");
    }

    public static MaterialSymbol iconQueryStats() {
        return symbols.get("query_stats");
    }

    public static MaterialSymbol iconQuestionAnswer() {
        return symbols.get("question_answer");
    }

    public static MaterialSymbol iconQuestionExchange() {
        return symbols.get("question_exchange");
    }

    public static MaterialSymbol iconQuestionMark() {
        return symbols.get("question_mark");
    }

    public static MaterialSymbol iconQueue() {
        return symbols.get("queue");
    }

    public static MaterialSymbol iconQueueMusic() {
        return symbols.get("queue_music");
    }

    public static MaterialSymbol iconQueuePlayNext() {
        return symbols.get("queue_play_next");
    }

    public static MaterialSymbol iconQuickPhrases() {
        return symbols.get("quick_phrases");
    }

    public static MaterialSymbol iconQuickReference() {
        return symbols.get("quick_reference");
    }

    public static MaterialSymbol iconQuickReferenceAll() {
        return symbols.get("quick_reference_all");
    }

    public static MaterialSymbol iconQuickReorder() {
        return symbols.get("quick_reorder");
    }

    public static MaterialSymbol iconQuickreply() {
        return symbols.get("quickreply");
    }

    public static MaterialSymbol iconQuietTime() {
        return symbols.get("quiet_time");
    }

    public static MaterialSymbol iconQuietTimeActive() {
        return symbols.get("quiet_time_active");
    }

    public static MaterialSymbol iconQuiz() {
        return symbols.get("quiz");
    }

    public static MaterialSymbol iconRMobiledata() {
        return symbols.get("r_mobiledata");
    }

    public static MaterialSymbol iconRadar() {
        return symbols.get("radar");
    }

    public static MaterialSymbol iconRadio() {
        return symbols.get("radio");
    }

    public static MaterialSymbol iconRadioButtonChecked() {
        return symbols.get("radio_button_checked");
    }

    public static MaterialSymbol iconRadioButtonPartial() {
        return symbols.get("radio_button_partial");
    }

    public static MaterialSymbol iconRadioButtonUnchecked() {
        return symbols.get("radio_button_unchecked");
    }

    public static MaterialSymbol iconRadiology() {
        return symbols.get("radiology");
    }

    public static MaterialSymbol iconRailwayAlert() {
        return symbols.get("railway_alert");
    }

    public static MaterialSymbol iconRailwayAlert2() {
        return symbols.get("railway_alert_2");
    }

    public static MaterialSymbol iconRainy() {
        return symbols.get("rainy");
    }

    public static MaterialSymbol iconRainyHeavy() {
        return symbols.get("rainy_heavy");
    }

    public static MaterialSymbol iconRainyLight() {
        return symbols.get("rainy_light");
    }

    public static MaterialSymbol iconRainySnow() {
        return symbols.get("rainy_snow");
    }

    public static MaterialSymbol iconRamenDining() {
        return symbols.get("ramen_dining");
    }

    public static MaterialSymbol iconRampLeft() {
        return symbols.get("ramp_left");
    }

    public static MaterialSymbol iconRampRight() {
        return symbols.get("ramp_right");
    }

    public static MaterialSymbol iconRangeHood() {
        return symbols.get("range_hood");
    }

    public static MaterialSymbol iconRateReview() {
        return symbols.get("rate_review");
    }

    public static MaterialSymbol iconRateReviewRtl() {
        return symbols.get("rate_review_rtl");
    }

    public static MaterialSymbol iconRaven() {
        return symbols.get("raven");
    }

    public static MaterialSymbol iconRawOff() {
        return symbols.get("raw_off");
    }

    public static MaterialSymbol iconRawOn() {
        return symbols.get("raw_on");
    }

    public static MaterialSymbol iconReadMore() {
        return symbols.get("read_more");
    }

    public static MaterialSymbol iconReadinessScore() {
        return symbols.get("readiness_score");
    }

    public static MaterialSymbol iconRealEstateAgent() {
        return symbols.get("real_estate_agent");
    }

    public static MaterialSymbol iconRearCamera() {
        return symbols.get("rear_camera");
    }

    public static MaterialSymbol iconRebase() {
        return symbols.get("rebase");
    }

    public static MaterialSymbol iconRebaseEdit() {
        return symbols.get("rebase_edit");
    }

    public static MaterialSymbol iconReceipt() {
        return symbols.get("receipt");
    }

    public static MaterialSymbol iconReceiptLong() {
        return symbols.get("receipt_long");
    }

    public static MaterialSymbol iconReceiptLongOff() {
        return symbols.get("receipt_long_off");
    }

    public static MaterialSymbol iconRecentActors() {
        return symbols.get("recent_actors");
    }

    public static MaterialSymbol iconRecentPatient() {
        return symbols.get("recent_patient");
    }

    public static MaterialSymbol iconRecenter() {
        return symbols.get("recenter");
    }

    public static MaterialSymbol iconRecommend() {
        return symbols.get("recommend");
    }

    public static MaterialSymbol iconRecordVoiceOver() {
        return symbols.get("record_voice_over");
    }

    public static MaterialSymbol iconRectangle() {
        return symbols.get("rectangle");
    }

    public static MaterialSymbol iconRecycling() {
        return symbols.get("recycling");
    }

    public static MaterialSymbol iconRedeem() {
        return symbols.get("redeem");
    }

    public static MaterialSymbol iconRedo() {
        return symbols.get("redo");
    }

    public static MaterialSymbol iconReduceCapacity() {
        return symbols.get("reduce_capacity");
    }

    public static MaterialSymbol iconRefresh() {
        return symbols.get("refresh");
    }

    public static MaterialSymbol iconRegularExpression() {
        return symbols.get("regular_expression");
    }

    public static MaterialSymbol iconRelax() {
        return symbols.get("relax");
    }

    public static MaterialSymbol iconReleaseAlert() {
        return symbols.get("release_alert");
    }

    public static MaterialSymbol iconRememberMe() {
        return symbols.get("remember_me");
    }

    public static MaterialSymbol iconReminder() {
        return symbols.get("reminder");
    }

    public static MaterialSymbol iconRemindersAlt() {
        return symbols.get("reminders_alt");
    }

    public static MaterialSymbol iconRemoteGen() {
        return symbols.get("remote_gen");
    }

    public static MaterialSymbol iconRemove() {
        return symbols.get("remove");
    }

    public static MaterialSymbol iconRemoveCircle() {
        return symbols.get("remove_circle");
    }

    public static MaterialSymbol iconRemoveCircleOutline() {
        return symbols.get("remove_circle_outline");
    }

    public static MaterialSymbol iconRemoveDone() {
        return symbols.get("remove_done");
    }

    public static MaterialSymbol iconRemoveFromQueue() {
        return symbols.get("remove_from_queue");
    }

    public static MaterialSymbol iconRemoveModerator() {
        return symbols.get("remove_moderator");
    }

    public static MaterialSymbol iconRemoveRedEye() {
        return symbols.get("remove_red_eye");
    }

    public static MaterialSymbol iconRemoveRoad() {
        return symbols.get("remove_road");
    }

    public static MaterialSymbol iconRemoveSelection() {
        return symbols.get("remove_selection");
    }

    public static MaterialSymbol iconRemoveShoppingCart() {
        return symbols.get("remove_shopping_cart");
    }

    public static MaterialSymbol iconReopenWindow() {
        return symbols.get("reopen_window");
    }

    public static MaterialSymbol iconReorder() {
        return symbols.get("reorder");
    }

    public static MaterialSymbol iconRepartition() {
        return symbols.get("repartition");
    }

    public static MaterialSymbol iconRepeat() {
        return symbols.get("repeat");
    }

    public static MaterialSymbol iconRepeatOn() {
        return symbols.get("repeat_on");
    }

    public static MaterialSymbol iconRepeatOne() {
        return symbols.get("repeat_one");
    }

    public static MaterialSymbol iconRepeatOneOn() {
        return symbols.get("repeat_one_on");
    }

    public static MaterialSymbol iconReplaceAudio() {
        return symbols.get("replace_audio");
    }

    public static MaterialSymbol iconReplaceImage() {
        return symbols.get("replace_image");
    }

    public static MaterialSymbol iconReplaceVideo() {
        return symbols.get("replace_video");
    }

    public static MaterialSymbol iconReplay() {
        return symbols.get("replay");
    }

    public static MaterialSymbol iconReplay10() {
        return symbols.get("replay_10");
    }

    public static MaterialSymbol iconReplay30() {
        return symbols.get("replay_30");
    }

    public static MaterialSymbol iconReplay5() {
        return symbols.get("replay_5");
    }

    public static MaterialSymbol iconReplayCircleFilled() {
        return symbols.get("replay_circle_filled");
    }

    public static MaterialSymbol iconReply() {
        return symbols.get("reply");
    }

    public static MaterialSymbol iconReplyAll() {
        return symbols.get("reply_all");
    }

    public static MaterialSymbol iconReport() {
        return symbols.get("report");
    }

    public static MaterialSymbol iconReportGmailerrorred() {
        return symbols.get("report_gmailerrorred");
    }

    public static MaterialSymbol iconReportOff() {
        return symbols.get("report_off");
    }

    public static MaterialSymbol iconReportProblem() {
        return symbols.get("report_problem");
    }

    public static MaterialSymbol iconRequestPage() {
        return symbols.get("request_page");
    }

    public static MaterialSymbol iconRequestQuote() {
        return symbols.get("request_quote");
    }

    public static MaterialSymbol iconResetBrightness() {
        return symbols.get("reset_brightness");
    }

    public static MaterialSymbol iconResetExposure() {
        return symbols.get("reset_exposure");
    }

    public static MaterialSymbol iconResetFocus() {
        return symbols.get("reset_focus");
    }

    public static MaterialSymbol iconResetImage() {
        return symbols.get("reset_image");
    }

    public static MaterialSymbol iconResetIso() {
        return symbols.get("reset_iso");
    }

    public static MaterialSymbol iconResetSettings() {
        return symbols.get("reset_settings");
    }

    public static MaterialSymbol iconResetShadow() {
        return symbols.get("reset_shadow");
    }

    public static MaterialSymbol iconResetShutterSpeed() {
        return symbols.get("reset_shutter_speed");
    }

    public static MaterialSymbol iconResetTv() {
        return symbols.get("reset_tv");
    }

    public static MaterialSymbol iconResetWhiteBalance() {
        return symbols.get("reset_white_balance");
    }

    public static MaterialSymbol iconResetWrench() {
        return symbols.get("reset_wrench");
    }

    public static MaterialSymbol iconResize() {
        return symbols.get("resize");
    }

    public static MaterialSymbol iconRespiratoryRate() {
        return symbols.get("respiratory_rate");
    }

    public static MaterialSymbol iconResponsiveLayout() {
        return symbols.get("responsive_layout");
    }

    public static MaterialSymbol iconRestArea() {
        return symbols.get("rest_area");
    }

    public static MaterialSymbol iconRestartAlt() {
        return symbols.get("restart_alt");
    }

    public static MaterialSymbol iconRestaurant() {
        return symbols.get("restaurant");
    }

    public static MaterialSymbol iconRestaurantMenu() {
        return symbols.get("restaurant_menu");
    }

    public static MaterialSymbol iconRestore() {
        return symbols.get("restore");
    }

    public static MaterialSymbol iconRestoreFromTrash() {
        return symbols.get("restore_from_trash");
    }

    public static MaterialSymbol iconRestorePage() {
        return symbols.get("restore_page");
    }

    public static MaterialSymbol iconResume() {
        return symbols.get("resume");
    }

    public static MaterialSymbol iconReviews() {
        return symbols.get("reviews");
    }

    public static MaterialSymbol iconRewardedAds() {
        return symbols.get("rewarded_ads");
    }

    public static MaterialSymbol iconRheumatology() {
        return symbols.get("rheumatology");
    }

    public static MaterialSymbol iconRibCage() {
        return symbols.get("rib_cage");
    }

    public static MaterialSymbol iconRiceBowl() {
        return symbols.get("rice_bowl");
    }

    public static MaterialSymbol iconRightClick() {
        return symbols.get("right_click");
    }

    public static MaterialSymbol iconRightPanelClose() {
        return symbols.get("right_panel_close");
    }

    public static MaterialSymbol iconRightPanelOpen() {
        return symbols.get("right_panel_open");
    }

    public static MaterialSymbol iconRingVolume() {
        return symbols.get("ring_volume");
    }

    public static MaterialSymbol iconRingVolumeFilled() {
        return symbols.get("ring_volume_filled");
    }

    public static MaterialSymbol iconRipples() {
        return symbols.get("ripples");
    }

    public static MaterialSymbol iconRoad() {
        return symbols.get("road");
    }

    public static MaterialSymbol iconRobot() {
        return symbols.get("robot");
    }

    public static MaterialSymbol iconRobot2() {
        return symbols.get("robot_2");
    }

    public static MaterialSymbol iconRocket() {
        return symbols.get("rocket");
    }

    public static MaterialSymbol iconRocketLaunch() {
        return symbols.get("rocket_launch");
    }

    public static MaterialSymbol iconRollerShades() {
        return symbols.get("roller_shades");
    }

    public static MaterialSymbol iconRollerShadesClosed() {
        return symbols.get("roller_shades_closed");
    }

    public static MaterialSymbol iconRollerSkating() {
        return symbols.get("roller_skating");
    }

    public static MaterialSymbol iconRoofing() {
        return symbols.get("roofing");
    }

    public static MaterialSymbol iconRoom() {
        return symbols.get("room");
    }

    public static MaterialSymbol iconRoomPreferences() {
        return symbols.get("room_preferences");
    }

    public static MaterialSymbol iconRoomService() {
        return symbols.get("room_service");
    }

    public static MaterialSymbol iconRotate90DegreesCcw() {
        return symbols.get("rotate_90_degrees_ccw");
    }

    public static MaterialSymbol iconRotate90DegreesCw() {
        return symbols.get("rotate_90_degrees_cw");
    }

    public static MaterialSymbol iconRotateAuto() {
        return symbols.get("rotate_auto");
    }

    public static MaterialSymbol iconRotateLeft() {
        return symbols.get("rotate_left");
    }

    public static MaterialSymbol iconRotateRight() {
        return symbols.get("rotate_right");
    }

    public static MaterialSymbol iconRoundaboutLeft() {
        return symbols.get("roundabout_left");
    }

    public static MaterialSymbol iconRoundaboutRight() {
        return symbols.get("roundabout_right");
    }

    public static MaterialSymbol iconRoundedCorner() {
        return symbols.get("rounded_corner");
    }

    public static MaterialSymbol iconRoute() {
        return symbols.get("route");
    }

    public static MaterialSymbol iconRouter() {
        return symbols.get("router");
    }

    public static MaterialSymbol iconRouterOff() {
        return symbols.get("router_off");
    }

    public static MaterialSymbol iconRoutine() {
        return symbols.get("routine");
    }

    public static MaterialSymbol iconRowing() {
        return symbols.get("rowing");
    }

    public static MaterialSymbol iconRssFeed() {
        return symbols.get("rss_feed");
    }

    public static MaterialSymbol iconRsvp() {
        return symbols.get("rsvp");
    }

    public static MaterialSymbol iconRtt() {
        return symbols.get("rtt");
    }

    public static MaterialSymbol iconRubric() {
        return symbols.get("rubric");
    }

    public static MaterialSymbol iconRule() {
        return symbols.get("rule");
    }

    public static MaterialSymbol iconRuleFolder() {
        return symbols.get("rule_folder");
    }

    public static MaterialSymbol iconRuleSettings() {
        return symbols.get("rule_settings");
    }

    public static MaterialSymbol iconRunCircle() {
        return symbols.get("run_circle");
    }

    public static MaterialSymbol iconRunningWithErrors() {
        return symbols.get("running_with_errors");
    }

    public static MaterialSymbol iconRvHookup() {
        return symbols.get("rv_hookup");
    }

    public static MaterialSymbol iconSafetyCheck() {
        return symbols.get("safety_check");
    }

    public static MaterialSymbol iconSafetyCheckOff() {
        return symbols.get("safety_check_off");
    }

    public static MaterialSymbol iconSafetyDivider() {
        return symbols.get("safety_divider");
    }

    public static MaterialSymbol iconSailing() {
        return symbols.get("sailing");
    }

    public static MaterialSymbol iconSalinity() {
        return symbols.get("salinity");
    }

    public static MaterialSymbol iconSanitizer() {
        return symbols.get("sanitizer");
    }

    public static MaterialSymbol iconSatellite() {
        return symbols.get("satellite");
    }

    public static MaterialSymbol iconSatelliteAlt() {
        return symbols.get("satellite_alt");
    }

    public static MaterialSymbol iconSauna() {
        return symbols.get("sauna");
    }

    public static MaterialSymbol iconSave() {
        return symbols.get("save");
    }

    public static MaterialSymbol iconSaveAlt() {
        return symbols.get("save_alt");
    }

    public static MaterialSymbol iconSaveAs() {
        return symbols.get("save_as");
    }

    public static MaterialSymbol iconSaveClock() {
        return symbols.get("save_clock");
    }

    public static MaterialSymbol iconSavedSearch() {
        return symbols.get("saved_search");
    }

    public static MaterialSymbol iconSavings() {
        return symbols.get("savings");
    }

    public static MaterialSymbol iconScale() {
        return symbols.get("scale");
    }

    public static MaterialSymbol iconScan() {
        return symbols.get("scan");
    }

    public static MaterialSymbol iconScanDelete() {
        return symbols.get("scan_delete");
    }

    public static MaterialSymbol iconScanner() {
        return symbols.get("scanner");
    }

    public static MaterialSymbol iconScatterPlot() {
        return symbols.get("scatter_plot");
    }

    public static MaterialSymbol iconScene() {
        return symbols.get("scene");
    }

    public static MaterialSymbol iconSchedule() {
        return symbols.get("schedule");
    }

    public static MaterialSymbol iconScheduleSend() {
        return symbols.get("schedule_send");
    }

    public static MaterialSymbol iconSchema() {
        return symbols.get("schema");
    }

    public static MaterialSymbol iconSchool() {
        return symbols.get("school");
    }

    public static MaterialSymbol iconScience() {
        return symbols.get("science");
    }

    public static MaterialSymbol iconScienceOff() {
        return symbols.get("science_off");
    }

    public static MaterialSymbol iconScooter() {
        return symbols.get("scooter");
    }

    public static MaterialSymbol iconScore() {
        return symbols.get("score");
    }

    public static MaterialSymbol iconScoreboard() {
        return symbols.get("scoreboard");
    }

    public static MaterialSymbol iconScreenLockLandscape() {
        return symbols.get("screen_lock_landscape");
    }

    public static MaterialSymbol iconScreenLockPortrait() {
        return symbols.get("screen_lock_portrait");
    }

    public static MaterialSymbol iconScreenLockRotation() {
        return symbols.get("screen_lock_rotation");
    }

    public static MaterialSymbol iconScreenRecord() {
        return symbols.get("screen_record");
    }

    public static MaterialSymbol iconScreenRotation() {
        return symbols.get("screen_rotation");
    }

    public static MaterialSymbol iconScreenRotationAlt() {
        return symbols.get("screen_rotation_alt");
    }

    public static MaterialSymbol iconScreenRotationUp() {
        return symbols.get("screen_rotation_up");
    }

    public static MaterialSymbol iconScreenSearchDesktop() {
        return symbols.get("screen_search_desktop");
    }

    public static MaterialSymbol iconScreenShare() {
        return symbols.get("screen_share");
    }

    public static MaterialSymbol iconScreenshot() {
        return symbols.get("screenshot");
    }

    public static MaterialSymbol iconScreenshotFrame() {
        return symbols.get("screenshot_frame");
    }

    public static MaterialSymbol iconScreenshotFrame2() {
        return symbols.get("screenshot_frame_2");
    }

    public static MaterialSymbol iconScreenshotKeyboard() {
        return symbols.get("screenshot_keyboard");
    }

    public static MaterialSymbol iconScreenshotMonitor() {
        return symbols.get("screenshot_monitor");
    }

    public static MaterialSymbol iconScreenshotRegion() {
        return symbols.get("screenshot_region");
    }

    public static MaterialSymbol iconScreenshotTablet() {
        return symbols.get("screenshot_tablet");
    }

    public static MaterialSymbol iconScript() {
        return symbols.get("script");
    }

    public static MaterialSymbol iconScrollableHeader() {
        return symbols.get("scrollable_header");
    }

    public static MaterialSymbol iconScubaDiving() {
        return symbols.get("scuba_diving");
    }

    public static MaterialSymbol iconSd() {
        return symbols.get("sd");
    }

    public static MaterialSymbol iconSdCard() {
        return symbols.get("sd_card");
    }

    public static MaterialSymbol iconSdCardAlert() {
        return symbols.get("sd_card_alert");
    }

    public static MaterialSymbol iconSdStorage() {
        return symbols.get("sd_storage");
    }

    public static MaterialSymbol iconSdk() {
        return symbols.get("sdk");
    }

    public static MaterialSymbol iconSearch() {
        return symbols.get("search");
    }

    public static MaterialSymbol iconSearchActivity() {
        return symbols.get("search_activity");
    }

    public static MaterialSymbol iconSearchCheck() {
        return symbols.get("search_check");
    }

    public static MaterialSymbol iconSearchCheck2() {
        return symbols.get("search_check_2");
    }

    public static MaterialSymbol iconSearchGear() {
        return symbols.get("search_gear");
    }

    public static MaterialSymbol iconSearchHandsFree() {
        return symbols.get("search_hands_free");
    }

    public static MaterialSymbol iconSearchInsights() {
        return symbols.get("search_insights");
    }

    public static MaterialSymbol iconSearchOff() {
        return symbols.get("search_off");
    }

    public static MaterialSymbol iconSeatCoolLeft() {
        return symbols.get("seat_cool_left");
    }

    public static MaterialSymbol iconSeatCoolRight() {
        return symbols.get("seat_cool_right");
    }

    public static MaterialSymbol iconSeatHeatLeft() {
        return symbols.get("seat_heat_left");
    }

    public static MaterialSymbol iconSeatHeatRight() {
        return symbols.get("seat_heat_right");
    }

    public static MaterialSymbol iconSeatVentLeft() {
        return symbols.get("seat_vent_left");
    }

    public static MaterialSymbol iconSeatVentRight() {
        return symbols.get("seat_vent_right");
    }

    public static MaterialSymbol iconSecurity() {
        return symbols.get("security");
    }

    public static MaterialSymbol iconSecurityKey() {
        return symbols.get("security_key");
    }

    public static MaterialSymbol iconSecurityUpdate() {
        return symbols.get("security_update");
    }

    public static MaterialSymbol iconSecurityUpdateGood() {
        return symbols.get("security_update_good");
    }

    public static MaterialSymbol iconSecurityUpdateWarning() {
        return symbols.get("security_update_warning");
    }

    public static MaterialSymbol iconSegment() {
        return symbols.get("segment");
    }

    public static MaterialSymbol iconSelect() {
        return symbols.get("select");
    }

    public static MaterialSymbol iconSelectAll() {
        return symbols.get("select_all");
    }

    public static MaterialSymbol iconSelectCheckBox() {
        return symbols.get("select_check_box");
    }

    public static MaterialSymbol iconSelectToSpeak() {
        return symbols.get("select_to_speak");
    }

    public static MaterialSymbol iconSelectWindow() {
        return symbols.get("select_window");
    }

    public static MaterialSymbol iconSelectWindow2() {
        return symbols.get("select_window_2");
    }

    public static MaterialSymbol iconSelectWindowOff() {
        return symbols.get("select_window_off");
    }

    public static MaterialSymbol iconSelfCare() {
        return symbols.get("self_care");
    }

    public static MaterialSymbol iconSelfImprovement() {
        return symbols.get("self_improvement");
    }

    public static MaterialSymbol iconSell() {
        return symbols.get("sell");
    }

    public static MaterialSymbol iconSend() {
        return symbols.get("send");
    }

    public static MaterialSymbol iconSendAndArchive() {
        return symbols.get("send_and_archive");
    }

    public static MaterialSymbol iconSendMoney() {
        return symbols.get("send_money");
    }

    public static MaterialSymbol iconSendTimeExtension() {
        return symbols.get("send_time_extension");
    }

    public static MaterialSymbol iconSendToMobile() {
        return symbols.get("send_to_mobile");
    }

    public static MaterialSymbol iconSensorDoor() {
        return symbols.get("sensor_door");
    }

    public static MaterialSymbol iconSensorOccupied() {
        return symbols.get("sensor_occupied");
    }

    public static MaterialSymbol iconSensorWindow() {
        return symbols.get("sensor_window");
    }

    public static MaterialSymbol iconSensors() {
        return symbols.get("sensors");
    }

    public static MaterialSymbol iconSensorsKrx() {
        return symbols.get("sensors_krx");
    }

    public static MaterialSymbol iconSensorsKrxOff() {
        return symbols.get("sensors_krx_off");
    }

    public static MaterialSymbol iconSensorsOff() {
        return symbols.get("sensors_off");
    }

    public static MaterialSymbol iconSentimentCalm() {
        return symbols.get("sentiment_calm");
    }

    public static MaterialSymbol iconSentimentContent() {
        return symbols.get("sentiment_content");
    }

    public static MaterialSymbol iconSentimentDissatisfied() {
        return symbols.get("sentiment_dissatisfied");
    }

    public static MaterialSymbol iconSentimentExcited() {
        return symbols.get("sentiment_excited");
    }

    public static MaterialSymbol iconSentimentExtremelyDissatisfied() {
        return symbols.get("sentiment_extremely_dissatisfied");
    }

    public static MaterialSymbol iconSentimentFrustrated() {
        return symbols.get("sentiment_frustrated");
    }

    public static MaterialSymbol iconSentimentNeutral() {
        return symbols.get("sentiment_neutral");
    }

    public static MaterialSymbol iconSentimentSad() {
        return symbols.get("sentiment_sad");
    }

    public static MaterialSymbol iconSentimentSatisfied() {
        return symbols.get("sentiment_satisfied");
    }

    public static MaterialSymbol iconSentimentSatisfiedAlt() {
        return symbols.get("sentiment_satisfied_alt");
    }

    public static MaterialSymbol iconSentimentStressed() {
        return symbols.get("sentiment_stressed");
    }

    public static MaterialSymbol iconSentimentVeryDissatisfied() {
        return symbols.get("sentiment_very_dissatisfied");
    }

    public static MaterialSymbol iconSentimentVerySatisfied() {
        return symbols.get("sentiment_very_satisfied");
    }

    public static MaterialSymbol iconSentimentWorried() {
        return symbols.get("sentiment_worried");
    }

    public static MaterialSymbol iconSerif() {
        return symbols.get("serif");
    }

    public static MaterialSymbol iconServerPerson() {
        return symbols.get("server_person");
    }

    public static MaterialSymbol iconServiceToolbox() {
        return symbols.get("service_toolbox");
    }

    public static MaterialSymbol iconSetMeal() {
        return symbols.get("set_meal");
    }

    public static MaterialSymbol iconSettings() {
        return symbols.get("settings");
    }

    public static MaterialSymbol iconSettingsAccessibility() {
        return symbols.get("settings_accessibility");
    }

    public static MaterialSymbol iconSettingsAccountBox() {
        return symbols.get("settings_account_box");
    }

    public static MaterialSymbol iconSettingsAlert() {
        return symbols.get("settings_alert");
    }

    public static MaterialSymbol iconSettingsApplications() {
        return symbols.get("settings_applications");
    }

    public static MaterialSymbol iconSettingsBRoll() {
        return symbols.get("settings_b_roll");
    }

    public static MaterialSymbol iconSettingsBackupRestore() {
        return symbols.get("settings_backup_restore");
    }

    public static MaterialSymbol iconSettingsBluetooth() {
        return symbols.get("settings_bluetooth");
    }

    public static MaterialSymbol iconSettingsBrightness() {
        return symbols.get("settings_brightness");
    }

    public static MaterialSymbol iconSettingsCell() {
        return symbols.get("settings_cell");
    }

    public static MaterialSymbol iconSettingsCinematicBlur() {
        return symbols.get("settings_cinematic_blur");
    }

    public static MaterialSymbol iconSettingsEthernet() {
        return symbols.get("settings_ethernet");
    }

    public static MaterialSymbol iconSettingsHeart() {
        return symbols.get("settings_heart");
    }

    public static MaterialSymbol iconSettingsInputAntenna() {
        return symbols.get("settings_input_antenna");
    }

    public static MaterialSymbol iconSettingsInputComponent() {
        return symbols.get("settings_input_component");
    }

    public static MaterialSymbol iconSettingsInputComposite() {
        return symbols.get("settings_input_composite");
    }

    public static MaterialSymbol iconSettingsInputHdmi() {
        return symbols.get("settings_input_hdmi");
    }

    public static MaterialSymbol iconSettingsInputSvideo() {
        return symbols.get("settings_input_svideo");
    }

    public static MaterialSymbol iconSettingsMotionMode() {
        return symbols.get("settings_motion_mode");
    }

    public static MaterialSymbol iconSettingsNightSight() {
        return symbols.get("settings_night_sight");
    }

    public static MaterialSymbol iconSettingsOverscan() {
        return symbols.get("settings_overscan");
    }

    public static MaterialSymbol iconSettingsPanorama() {
        return symbols.get("settings_panorama");
    }

    public static MaterialSymbol iconSettingsPhone() {
        return symbols.get("settings_phone");
    }

    public static MaterialSymbol iconSettingsPhotoCamera() {
        return symbols.get("settings_photo_camera");
    }

    public static MaterialSymbol iconSettingsPower() {
        return symbols.get("settings_power");
    }

    public static MaterialSymbol iconSettingsRemote() {
        return symbols.get("settings_remote");
    }

    public static MaterialSymbol iconSettingsSeating() {
        return symbols.get("settings_seating");
    }

    public static MaterialSymbol iconSettingsSlowMotion() {
        return symbols.get("settings_slow_motion");
    }

    public static MaterialSymbol iconSettingsSuggest() {
        return symbols.get("settings_suggest");
    }

    public static MaterialSymbol iconSettingsSystemDaydream() {
        return symbols.get("settings_system_daydream");
    }

    public static MaterialSymbol iconSettingsTimelapse() {
        return symbols.get("settings_timelapse");
    }

    public static MaterialSymbol iconSettingsVideoCamera() {
        return symbols.get("settings_video_camera");
    }

    public static MaterialSymbol iconSettingsVoice() {
        return symbols.get("settings_voice");
    }

    public static MaterialSymbol iconSettopComponent() {
        return symbols.get("settop_component");
    }

    public static MaterialSymbol iconSevereCold() {
        return symbols.get("severe_cold");
    }

    public static MaterialSymbol iconShadow() {
        return symbols.get("shadow");
    }

    public static MaterialSymbol iconShadowAdd() {
        return symbols.get("shadow_add");
    }

    public static MaterialSymbol iconShadowMinus() {
        return symbols.get("shadow_minus");
    }

    public static MaterialSymbol iconShapeLine() {
        return symbols.get("shape_line");
    }

    public static MaterialSymbol iconShapeRecognition() {
        return symbols.get("shape_recognition");
    }

    public static MaterialSymbol iconShapes() {
        return symbols.get("shapes");
    }

    public static MaterialSymbol iconShare() {
        return symbols.get("share");
    }

    public static MaterialSymbol iconShareEta() {
        return symbols.get("share_eta");
    }

    public static MaterialSymbol iconShareLocation() {
        return symbols.get("share_location");
    }

    public static MaterialSymbol iconShareOff() {
        return symbols.get("share_off");
    }

    public static MaterialSymbol iconShareReviews() {
        return symbols.get("share_reviews");
    }

    public static MaterialSymbol iconShareWindows() {
        return symbols.get("share_windows");
    }

    public static MaterialSymbol iconShavedIce() {
        return symbols.get("shaved_ice");
    }

    public static MaterialSymbol iconSheetsRtl() {
        return symbols.get("sheets_rtl");
    }

    public static MaterialSymbol iconShelfAutoHide() {
        return symbols.get("shelf_auto_hide");
    }

    public static MaterialSymbol iconShelfPosition() {
        return symbols.get("shelf_position");
    }

    public static MaterialSymbol iconShelves() {
        return symbols.get("shelves");
    }

    public static MaterialSymbol iconShield() {
        return symbols.get("shield");
    }

    public static MaterialSymbol iconShieldLock() {
        return symbols.get("shield_lock");
    }

    public static MaterialSymbol iconShieldLocked() {
        return symbols.get("shield_locked");
    }

    public static MaterialSymbol iconShieldMoon() {
        return symbols.get("shield_moon");
    }

    public static MaterialSymbol iconShieldPerson() {
        return symbols.get("shield_person");
    }

    public static MaterialSymbol iconShieldQuestion() {
        return symbols.get("shield_question");
    }

    public static MaterialSymbol iconShieldToggle() {
        return symbols.get("shield_toggle");
    }

    public static MaterialSymbol iconShieldWatch() {
        return symbols.get("shield_watch");
    }

    public static MaterialSymbol iconShieldWithHeart() {
        return symbols.get("shield_with_heart");
    }

    public static MaterialSymbol iconShieldWithHouse() {
        return symbols.get("shield_with_house");
    }

    public static MaterialSymbol iconShift() {
        return symbols.get("shift");
    }

    public static MaterialSymbol iconShiftLock() {
        return symbols.get("shift_lock");
    }

    public static MaterialSymbol iconShiftLockOff() {
        return symbols.get("shift_lock_off");
    }

    public static MaterialSymbol iconShop() {
        return symbols.get("shop");
    }

    public static MaterialSymbol iconShop2() {
        return symbols.get("shop_2");
    }

    public static MaterialSymbol iconShopTwo() {
        return symbols.get("shop_two");
    }

    public static MaterialSymbol iconShoppingBag() {
        return symbols.get("shopping_bag");
    }

    public static MaterialSymbol iconShoppingBagSpeed() {
        return symbols.get("shopping_bag_speed");
    }

    public static MaterialSymbol iconShoppingBasket() {
        return symbols.get("shopping_basket");
    }

    public static MaterialSymbol iconShoppingCart() {
        return symbols.get("shopping_cart");
    }

    public static MaterialSymbol iconShoppingCartCheckout() {
        return symbols.get("shopping_cart_checkout");
    }

    public static MaterialSymbol iconShoppingCartOff() {
        return symbols.get("shopping_cart_off");
    }

    public static MaterialSymbol iconShoppingmode() {
        return symbols.get("shoppingmode");
    }

    public static MaterialSymbol iconShortStay() {
        return symbols.get("short_stay");
    }

    public static MaterialSymbol iconShortText() {
        return symbols.get("short_text");
    }

    public static MaterialSymbol iconShortcut() {
        return symbols.get("shortcut");
    }

    public static MaterialSymbol iconShowChart() {
        return symbols.get("show_chart");
    }

    public static MaterialSymbol iconShower() {
        return symbols.get("shower");
    }

    public static MaterialSymbol iconShuffle() {
        return symbols.get("shuffle");
    }

    public static MaterialSymbol iconShuffleOn() {
        return symbols.get("shuffle_on");
    }

    public static MaterialSymbol iconShutterSpeed() {
        return symbols.get("shutter_speed");
    }

    public static MaterialSymbol iconShutterSpeedAdd() {
        return symbols.get("shutter_speed_add");
    }

    public static MaterialSymbol iconShutterSpeedMinus() {
        return symbols.get("shutter_speed_minus");
    }

    public static MaterialSymbol iconSick() {
        return symbols.get("sick");
    }

    public static MaterialSymbol iconSideNavigation() {
        return symbols.get("side_navigation");
    }

    public static MaterialSymbol iconSignLanguage() {
        return symbols.get("sign_language");
    }

    public static MaterialSymbol iconSignLanguage2() {
        return symbols.get("sign_language_2");
    }

    public static MaterialSymbol iconSignalCellular0Bar() {
        return symbols.get("signal_cellular_0_bar");
    }

    public static MaterialSymbol iconSignalCellular1Bar() {
        return symbols.get("signal_cellular_1_bar");
    }

    public static MaterialSymbol iconSignalCellular2Bar() {
        return symbols.get("signal_cellular_2_bar");
    }

    public static MaterialSymbol iconSignalCellular3Bar() {
        return symbols.get("signal_cellular_3_bar");
    }

    public static MaterialSymbol iconSignalCellular4Bar() {
        return symbols.get("signal_cellular_4_bar");
    }

    public static MaterialSymbol iconSignalCellularAdd() {
        return symbols.get("signal_cellular_add");
    }

    public static MaterialSymbol iconSignalCellularAlt() {
        return symbols.get("signal_cellular_alt");
    }

    public static MaterialSymbol iconSignalCellularAlt1Bar() {
        return symbols.get("signal_cellular_alt_1_bar");
    }

    public static MaterialSymbol iconSignalCellularAlt2Bar() {
        return symbols.get("signal_cellular_alt_2_bar");
    }

    public static MaterialSymbol iconSignalCellularConnectedNoInternet0Bar() {
        return symbols.get("signal_cellular_connected_no_internet_0_bar");
    }

    public static MaterialSymbol iconSignalCellularConnectedNoInternet4Bar() {
        return symbols.get("signal_cellular_connected_no_internet_4_bar");
    }

    public static MaterialSymbol iconSignalCellularNoSim() {
        return symbols.get("signal_cellular_no_sim");
    }

    public static MaterialSymbol iconSignalCellularNodata() {
        return symbols.get("signal_cellular_nodata");
    }

    public static MaterialSymbol iconSignalCellularNull() {
        return symbols.get("signal_cellular_null");
    }

    public static MaterialSymbol iconSignalCellularOff() {
        return symbols.get("signal_cellular_off");
    }

    public static MaterialSymbol iconSignalCellularPause() {
        return symbols.get("signal_cellular_pause");
    }

    public static MaterialSymbol iconSignalDisconnected() {
        return symbols.get("signal_disconnected");
    }

    public static MaterialSymbol iconSignalWifi0Bar() {
        return symbols.get("signal_wifi_0_bar");
    }

    public static MaterialSymbol iconSignalWifi4Bar() {
        return symbols.get("signal_wifi_4_bar");
    }

    public static MaterialSymbol iconSignalWifi4BarLock() {
        return symbols.get("signal_wifi_4_bar_lock");
    }

    public static MaterialSymbol iconSignalWifiBad() {
        return symbols.get("signal_wifi_bad");
    }

    public static MaterialSymbol iconSignalWifiConnectedNoInternet4() {
        return symbols.get("signal_wifi_connected_no_internet_4");
    }

    public static MaterialSymbol iconSignalWifiOff() {
        return symbols.get("signal_wifi_off");
    }

    public static MaterialSymbol iconSignalWifiStatusbar4Bar() {
        return symbols.get("signal_wifi_statusbar_4_bar");
    }

    public static MaterialSymbol iconSignalWifiStatusbarNotConnected() {
        return symbols.get("signal_wifi_statusbar_not_connected");
    }

    public static MaterialSymbol iconSignalWifiStatusbarNull() {
        return symbols.get("signal_wifi_statusbar_null");
    }

    public static MaterialSymbol iconSignature() {
        return symbols.get("signature");
    }

    public static MaterialSymbol iconSignpost() {
        return symbols.get("signpost");
    }

    public static MaterialSymbol iconSimCard() {
        return symbols.get("sim_card");
    }

    public static MaterialSymbol iconSimCardAlert() {
        return symbols.get("sim_card_alert");
    }

    public static MaterialSymbol iconSimCardDownload() {
        return symbols.get("sim_card_download");
    }

    public static MaterialSymbol iconSimulation() {
        return symbols.get("simulation");
    }

    public static MaterialSymbol iconSingleBed() {
        return symbols.get("single_bed");
    }

    public static MaterialSymbol iconSip() {
        return symbols.get("sip");
    }

    public static MaterialSymbol iconSiren() {
        return symbols.get("siren");
    }

    public static MaterialSymbol iconSirenCheck() {
        return symbols.get("siren_check");
    }

    public static MaterialSymbol iconSirenOpen() {
        return symbols.get("siren_open");
    }

    public static MaterialSymbol iconSirenQuestion() {
        return symbols.get("siren_question");
    }

    public static MaterialSymbol iconSkateboarding() {
        return symbols.get("skateboarding");
    }

    public static MaterialSymbol iconSkeleton() {
        return symbols.get("skeleton");
    }

    public static MaterialSymbol iconSkillet() {
        return symbols.get("skillet");
    }

    public static MaterialSymbol iconSkilletCooktop() {
        return symbols.get("skillet_cooktop");
    }

    public static MaterialSymbol iconSkipNext() {
        return symbols.get("skip_next");
    }

    public static MaterialSymbol iconSkipPrevious() {
        return symbols.get("skip_previous");
    }

    public static MaterialSymbol iconSkull() {
        return symbols.get("skull");
    }

    public static MaterialSymbol iconSkullList() {
        return symbols.get("skull_list");
    }

    public static MaterialSymbol iconSlabSerif() {
        return symbols.get("slab_serif");
    }

    public static MaterialSymbol iconSledding() {
        return symbols.get("sledding");
    }

    public static MaterialSymbol iconSleep() {
        return symbols.get("sleep");
    }

    public static MaterialSymbol iconSleepScore() {
        return symbols.get("sleep_score");
    }

    public static MaterialSymbol iconSlideLibrary() {
        return symbols.get("slide_library");
    }

    public static MaterialSymbol iconSliders() {
        return symbols.get("sliders");
    }

    public static MaterialSymbol iconSlideshow() {
        return symbols.get("slideshow");
    }

    public static MaterialSymbol iconSlowMotionVideo() {
        return symbols.get("slow_motion_video");
    }

    public static MaterialSymbol iconSmartButton() {
        return symbols.get("smart_button");
    }

    public static MaterialSymbol iconSmartCardReader() {
        return symbols.get("smart_card_reader");
    }

    public static MaterialSymbol iconSmartCardReaderOff() {
        return symbols.get("smart_card_reader_off");
    }

    public static MaterialSymbol iconSmartDisplay() {
        return symbols.get("smart_display");
    }

    public static MaterialSymbol iconSmartOutlet() {
        return symbols.get("smart_outlet");
    }

    public static MaterialSymbol iconSmartScreen() {
        return symbols.get("smart_screen");
    }

    public static MaterialSymbol iconSmartToy() {
        return symbols.get("smart_toy");
    }

    public static MaterialSymbol iconSmartphone() {
        return symbols.get("smartphone");
    }

    public static MaterialSymbol iconSmartphoneCamera() {
        return symbols.get("smartphone_camera");
    }

    public static MaterialSymbol iconSmbShare() {
        return symbols.get("smb_share");
    }

    public static MaterialSymbol iconSmokeFree() {
        return symbols.get("smoke_free");
    }

    public static MaterialSymbol iconSmokingRooms() {
        return symbols.get("smoking_rooms");
    }

    public static MaterialSymbol iconSms() {
        return symbols.get("sms");
    }

    public static MaterialSymbol iconSmsFailed() {
        return symbols.get("sms_failed");
    }

    public static MaterialSymbol iconSnippetFolder() {
        return symbols.get("snippet_folder");
    }

    public static MaterialSymbol iconSnooze() {
        return symbols.get("snooze");
    }

    public static MaterialSymbol iconSnowboarding() {
        return symbols.get("snowboarding");
    }

    public static MaterialSymbol iconSnowing() {
        return symbols.get("snowing");
    }

    public static MaterialSymbol iconSnowingHeavy() {
        return symbols.get("snowing_heavy");
    }

    public static MaterialSymbol iconSnowmobile() {
        return symbols.get("snowmobile");
    }

    public static MaterialSymbol iconSnowshoeing() {
        return symbols.get("snowshoeing");
    }

    public static MaterialSymbol iconSoap() {
        return symbols.get("soap");
    }

    public static MaterialSymbol iconSoba() {
        return symbols.get("soba");
    }

    public static MaterialSymbol iconSocialDistance() {
        return symbols.get("social_distance");
    }

    public static MaterialSymbol iconSocialLeaderboard() {
        return symbols.get("social_leaderboard");
    }

    public static MaterialSymbol iconSolarPower() {
        return symbols.get("solar_power");
    }

    public static MaterialSymbol iconSoloDining() {
        return symbols.get("solo_dining");
    }

    public static MaterialSymbol iconSort() {
        return symbols.get("sort");
    }

    public static MaterialSymbol iconSortByAlpha() {
        return symbols.get("sort_by_alpha");
    }

    public static MaterialSymbol iconSos() {
        return symbols.get("sos");
    }

    public static MaterialSymbol iconSoundDetectionDogBarking() {
        return symbols.get("sound_detection_dog_barking");
    }

    public static MaterialSymbol iconSoundDetectionGlassBreak() {
        return symbols.get("sound_detection_glass_break");
    }

    public static MaterialSymbol iconSoundDetectionLoudSound() {
        return symbols.get("sound_detection_loud_sound");
    }

    public static MaterialSymbol iconSoundSampler() {
        return symbols.get("sound_sampler");
    }

    public static MaterialSymbol iconSoupKitchen() {
        return symbols.get("soup_kitchen");
    }

    public static MaterialSymbol iconSource() {
        return symbols.get("source");
    }

    public static MaterialSymbol iconSourceEnvironment() {
        return symbols.get("source_environment");
    }

    public static MaterialSymbol iconSourceNotes() {
        return symbols.get("source_notes");
    }

    public static MaterialSymbol iconSouth() {
        return symbols.get("south");
    }

    public static MaterialSymbol iconSouthAmerica() {
        return symbols.get("south_america");
    }

    public static MaterialSymbol iconSouthEast() {
        return symbols.get("south_east");
    }

    public static MaterialSymbol iconSouthWest() {
        return symbols.get("south_west");
    }

    public static MaterialSymbol iconSpa() {
        return symbols.get("spa");
    }

    public static MaterialSymbol iconSpaceBar() {
        return symbols.get("space_bar");
    }

    public static MaterialSymbol iconSpaceDashboard() {
        return symbols.get("space_dashboard");
    }

    public static MaterialSymbol iconSpatialAudio() {
        return symbols.get("spatial_audio");
    }

    public static MaterialSymbol iconSpatialAudioOff() {
        return symbols.get("spatial_audio_off");
    }

    public static MaterialSymbol iconSpatialSpeaker() {
        return symbols.get("spatial_speaker");
    }

    public static MaterialSymbol iconSpatialTracking() {
        return symbols.get("spatial_tracking");
    }

    public static MaterialSymbol iconSpeaker() {
        return symbols.get("speaker");
    }

    public static MaterialSymbol iconSpeakerGroup() {
        return symbols.get("speaker_group");
    }

    public static MaterialSymbol iconSpeakerNotes() {
        return symbols.get("speaker_notes");
    }

    public static MaterialSymbol iconSpeakerNotesOff() {
        return symbols.get("speaker_notes_off");
    }

    public static MaterialSymbol iconSpeakerPhone() {
        return symbols.get("speaker_phone");
    }

    public static MaterialSymbol iconSpecialCharacter() {
        return symbols.get("special_character");
    }

    public static MaterialSymbol iconSpecificGravity() {
        return symbols.get("specific_gravity");
    }

    public static MaterialSymbol iconSpeechToText() {
        return symbols.get("speech_to_text");
    }

    public static MaterialSymbol iconSpeed() {
        return symbols.get("speed");
    }

    public static MaterialSymbol iconSpeed025() {
        return symbols.get("speed_0_25");
    }

    public static MaterialSymbol iconSpeed02x() {
        return symbols.get("speed_0_2x");
    }

    public static MaterialSymbol iconSpeed05() {
        return symbols.get("speed_0_5");
    }

    public static MaterialSymbol iconSpeed05x() {
        return symbols.get("speed_0_5x");
    }

    public static MaterialSymbol iconSpeed075() {
        return symbols.get("speed_0_75");
    }

    public static MaterialSymbol iconSpeed07x() {
        return symbols.get("speed_0_7x");
    }

    public static MaterialSymbol iconSpeed12() {
        return symbols.get("speed_1_2");
    }

    public static MaterialSymbol iconSpeed125() {
        return symbols.get("speed_1_25");
    }

    public static MaterialSymbol iconSpeed12x() {
        return symbols.get("speed_1_2x");
    }

    public static MaterialSymbol iconSpeed15() {
        return symbols.get("speed_1_5");
    }

    public static MaterialSymbol iconSpeed15x() {
        return symbols.get("speed_1_5x");
    }

    public static MaterialSymbol iconSpeed175() {
        return symbols.get("speed_1_75");
    }

    public static MaterialSymbol iconSpeed17x() {
        return symbols.get("speed_1_7x");
    }

    public static MaterialSymbol iconSpeed2x() {
        return symbols.get("speed_2x");
    }

    public static MaterialSymbol iconSpeedCamera() {
        return symbols.get("speed_camera");
    }

    public static MaterialSymbol iconSpellcheck() {
        return symbols.get("spellcheck");
    }

    public static MaterialSymbol iconSplitScene() {
        return symbols.get("split_scene");
    }

    public static MaterialSymbol iconSplitSceneDown() {
        return symbols.get("split_scene_down");
    }

    public static MaterialSymbol iconSplitSceneLeft() {
        return symbols.get("split_scene_left");
    }

    public static MaterialSymbol iconSplitSceneRight() {
        return symbols.get("split_scene_right");
    }

    public static MaterialSymbol iconSplitSceneUp() {
        return symbols.get("split_scene_up");
    }

    public static MaterialSymbol iconSplitscreen() {
        return symbols.get("splitscreen");
    }

    public static MaterialSymbol iconSplitscreenAdd() {
        return symbols.get("splitscreen_add");
    }

    public static MaterialSymbol iconSplitscreenBottom() {
        return symbols.get("splitscreen_bottom");
    }

    public static MaterialSymbol iconSplitscreenLandscape() {
        return symbols.get("splitscreen_landscape");
    }

    public static MaterialSymbol iconSplitscreenLeft() {
        return symbols.get("splitscreen_left");
    }

    public static MaterialSymbol iconSplitscreenPortrait() {
        return symbols.get("splitscreen_portrait");
    }

    public static MaterialSymbol iconSplitscreenRight() {
        return symbols.get("splitscreen_right");
    }

    public static MaterialSymbol iconSplitscreenTop() {
        return symbols.get("splitscreen_top");
    }

    public static MaterialSymbol iconSplitscreenVerticalAdd() {
        return symbols.get("splitscreen_vertical_add");
    }

    public static MaterialSymbol iconSpo2() {
        return symbols.get("spo2");
    }

    public static MaterialSymbol iconSpoke() {
        return symbols.get("spoke");
    }

    public static MaterialSymbol iconSports() {
        return symbols.get("sports");
    }

    public static MaterialSymbol iconSportsAndOutdoors() {
        return symbols.get("sports_and_outdoors");
    }

    public static MaterialSymbol iconSportsBar() {
        return symbols.get("sports_bar");
    }

    public static MaterialSymbol iconSportsBaseball() {
        return symbols.get("sports_baseball");
    }

    public static MaterialSymbol iconSportsBasketball() {
        return symbols.get("sports_basketball");
    }

    public static MaterialSymbol iconSportsCricket() {
        return symbols.get("sports_cricket");
    }

    public static MaterialSymbol iconSportsEsports() {
        return symbols.get("sports_esports");
    }

    public static MaterialSymbol iconSportsFootball() {
        return symbols.get("sports_football");
    }

    public static MaterialSymbol iconSportsGolf() {
        return symbols.get("sports_golf");
    }

    public static MaterialSymbol iconSportsGymnastics() {
        return symbols.get("sports_gymnastics");
    }

    public static MaterialSymbol iconSportsHandball() {
        return symbols.get("sports_handball");
    }

    public static MaterialSymbol iconSportsHockey() {
        return symbols.get("sports_hockey");
    }

    public static MaterialSymbol iconSportsKabaddi() {
        return symbols.get("sports_kabaddi");
    }

    public static MaterialSymbol iconSportsMartialArts() {
        return symbols.get("sports_martial_arts");
    }

    public static MaterialSymbol iconSportsMma() {
        return symbols.get("sports_mma");
    }

    public static MaterialSymbol iconSportsMotorsports() {
        return symbols.get("sports_motorsports");
    }

    public static MaterialSymbol iconSportsRugby() {
        return symbols.get("sports_rugby");
    }

    public static MaterialSymbol iconSportsScore() {
        return symbols.get("sports_score");
    }

    public static MaterialSymbol iconSportsSoccer() {
        return symbols.get("sports_soccer");
    }

    public static MaterialSymbol iconSportsTennis() {
        return symbols.get("sports_tennis");
    }

    public static MaterialSymbol iconSportsVolleyball() {
        return symbols.get("sports_volleyball");
    }

    public static MaterialSymbol iconSprinkler() {
        return symbols.get("sprinkler");
    }

    public static MaterialSymbol iconSprint() {
        return symbols.get("sprint");
    }

    public static MaterialSymbol iconSquare() {
        return symbols.get("square");
    }

    public static MaterialSymbol iconSquareDot() {
        return symbols.get("square_dot");
    }

    public static MaterialSymbol iconSquareFoot() {
        return symbols.get("square_foot");
    }

    public static MaterialSymbol iconSsidChart() {
        return symbols.get("ssid_chart");
    }

    public static MaterialSymbol iconStack() {
        return symbols.get("stack");
    }

    public static MaterialSymbol iconStackGroup() {
        return symbols.get("stack_group");
    }

    public static MaterialSymbol iconStackHexagon() {
        return symbols.get("stack_hexagon");
    }

    public static MaterialSymbol iconStackOff() {
        return symbols.get("stack_off");
    }

    public static MaterialSymbol iconStackStar() {
        return symbols.get("stack_star");
    }

    public static MaterialSymbol iconStackedBarChart() {
        return symbols.get("stacked_bar_chart");
    }

    public static MaterialSymbol iconStackedEmail() {
        return symbols.get("stacked_email");
    }

    public static MaterialSymbol iconStackedInbox() {
        return symbols.get("stacked_inbox");
    }

    public static MaterialSymbol iconStackedLineChart() {
        return symbols.get("stacked_line_chart");
    }

    public static MaterialSymbol iconStacks() {
        return symbols.get("stacks");
    }

    public static MaterialSymbol iconStadiaController() {
        return symbols.get("stadia_controller");
    }

    public static MaterialSymbol iconStadium() {
        return symbols.get("stadium");
    }

    public static MaterialSymbol iconStairs() {
        return symbols.get("stairs");
    }

    public static MaterialSymbol iconStairs2() {
        return symbols.get("stairs_2");
    }

    public static MaterialSymbol iconStar() {
        return symbols.get("star");
    }

    public static MaterialSymbol iconStarBorder() {
        return symbols.get("star_border");
    }

    public static MaterialSymbol iconStarBorderPurple500() {
        return symbols.get("star_border_purple500");
    }

    public static MaterialSymbol iconStarHalf() {
        return symbols.get("star_half");
    }

    public static MaterialSymbol iconStarOutline() {
        return symbols.get("star_outline");
    }

    public static MaterialSymbol iconStarPurple500() {
        return symbols.get("star_purple500");
    }

    public static MaterialSymbol iconStarRate() {
        return symbols.get("star_rate");
    }

    public static MaterialSymbol iconStarRateHalf() {
        return symbols.get("star_rate_half");
    }

    public static MaterialSymbol iconStarShine() {
        return symbols.get("star_shine");
    }

    public static MaterialSymbol iconStars() {
        return symbols.get("stars");
    }

    public static MaterialSymbol iconStars2() {
        return symbols.get("stars_2");
    }

    public static MaterialSymbol iconStart() {
        return symbols.get("start");
    }

    public static MaterialSymbol iconStat0() {
        return symbols.get("stat_0");
    }

    public static MaterialSymbol iconStat1() {
        return symbols.get("stat_1");
    }

    public static MaterialSymbol iconStat2() {
        return symbols.get("stat_2");
    }

    public static MaterialSymbol iconStat3() {
        return symbols.get("stat_3");
    }

    public static MaterialSymbol iconStatMinus1() {
        return symbols.get("stat_minus_1");
    }

    public static MaterialSymbol iconStatMinus2() {
        return symbols.get("stat_minus_2");
    }

    public static MaterialSymbol iconStatMinus3() {
        return symbols.get("stat_minus_3");
    }

    public static MaterialSymbol iconStayCurrentLandscape() {
        return symbols.get("stay_current_landscape");
    }

    public static MaterialSymbol iconStayCurrentPortrait() {
        return symbols.get("stay_current_portrait");
    }

    public static MaterialSymbol iconStayPrimaryLandscape() {
        return symbols.get("stay_primary_landscape");
    }

    public static MaterialSymbol iconStayPrimaryPortrait() {
        return symbols.get("stay_primary_portrait");
    }

    public static MaterialSymbol iconSteeringWheelHeat() {
        return symbols.get("steering_wheel_heat");
    }

    public static MaterialSymbol iconStep() {
        return symbols.get("step");
    }

    public static MaterialSymbol iconStepInto() {
        return symbols.get("step_into");
    }

    public static MaterialSymbol iconStepOut() {
        return symbols.get("step_out");
    }

    public static MaterialSymbol iconStepOver() {
        return symbols.get("step_over");
    }

    public static MaterialSymbol iconSteppers() {
        return symbols.get("steppers");
    }

    public static MaterialSymbol iconSteps() {
        return symbols.get("steps");
    }

    public static MaterialSymbol iconStethoscope() {
        return symbols.get("stethoscope");
    }

    public static MaterialSymbol iconStethoscopeArrow() {
        return symbols.get("stethoscope_arrow");
    }

    public static MaterialSymbol iconStethoscopeCheck() {
        return symbols.get("stethoscope_check");
    }

    public static MaterialSymbol iconStickyNote() {
        return symbols.get("sticky_note");
    }

    public static MaterialSymbol iconStickyNote2() {
        return symbols.get("sticky_note_2");
    }

    public static MaterialSymbol iconStockMedia() {
        return symbols.get("stock_media");
    }

    public static MaterialSymbol iconStockpot() {
        return symbols.get("stockpot");
    }

    public static MaterialSymbol iconStop() {
        return symbols.get("stop");
    }

    public static MaterialSymbol iconStopCircle() {
        return symbols.get("stop_circle");
    }

    public static MaterialSymbol iconStopScreenShare() {
        return symbols.get("stop_screen_share");
    }

    public static MaterialSymbol iconStorage() {
        return symbols.get("storage");
    }

    public static MaterialSymbol iconStore() {
        return symbols.get("store");
    }

    public static MaterialSymbol iconStoreMallDirectory() {
        return symbols.get("store_mall_directory");
    }

    public static MaterialSymbol iconStorefront() {
        return symbols.get("storefront");
    }

    public static MaterialSymbol iconStorm() {
        return symbols.get("storm");
    }

    public static MaterialSymbol iconStraight() {
        return symbols.get("straight");
    }

    public static MaterialSymbol iconStraighten() {
        return symbols.get("straighten");
    }

    public static MaterialSymbol iconStrategy() {
        return symbols.get("strategy");
    }

    public static MaterialSymbol iconStream() {
        return symbols.get("stream");
    }

    public static MaterialSymbol iconStreamApps() {
        return symbols.get("stream_apps");
    }

    public static MaterialSymbol iconStreetview() {
        return symbols.get("streetview");
    }

    public static MaterialSymbol iconStressManagement() {
        return symbols.get("stress_management");
    }

    public static MaterialSymbol iconStrikethroughS() {
        return symbols.get("strikethrough_s");
    }

    public static MaterialSymbol iconStrokeFull() {
        return symbols.get("stroke_full");
    }

    public static MaterialSymbol iconStrokePartial() {
        return symbols.get("stroke_partial");
    }

    public static MaterialSymbol iconStroller() {
        return symbols.get("stroller");
    }

    public static MaterialSymbol iconStyle() {
        return symbols.get("style");
    }

    public static MaterialSymbol iconStyler() {
        return symbols.get("styler");
    }

    public static MaterialSymbol iconStylus() {
        return symbols.get("stylus");
    }

    public static MaterialSymbol iconStylusBrush() {
        return symbols.get("stylus_brush");
    }

    public static MaterialSymbol iconStylusFountainPen() {
        return symbols.get("stylus_fountain_pen");
    }

    public static MaterialSymbol iconStylusHighlighter() {
        return symbols.get("stylus_highlighter");
    }

    public static MaterialSymbol iconStylusLaserPointer() {
        return symbols.get("stylus_laser_pointer");
    }

    public static MaterialSymbol iconStylusNote() {
        return symbols.get("stylus_note");
    }

    public static MaterialSymbol iconStylusPen() {
        return symbols.get("stylus_pen");
    }

    public static MaterialSymbol iconStylusPencil() {
        return symbols.get("stylus_pencil");
    }

    public static MaterialSymbol iconSubdirectoryArrowLeft() {
        return symbols.get("subdirectory_arrow_left");
    }

    public static MaterialSymbol iconSubdirectoryArrowRight() {
        return symbols.get("subdirectory_arrow_right");
    }

    public static MaterialSymbol iconSubheader() {
        return symbols.get("subheader");
    }

    public static MaterialSymbol iconSubject() {
        return symbols.get("subject");
    }

    public static MaterialSymbol iconSubscript() {
        return symbols.get("subscript");
    }

    public static MaterialSymbol iconSubscriptions() {
        return symbols.get("subscriptions");
    }

    public static MaterialSymbol iconSubtitles() {
        return symbols.get("subtitles");
    }

    public static MaterialSymbol iconSubtitlesGear() {
        return symbols.get("subtitles_gear");
    }

    public static MaterialSymbol iconSubtitlesOff() {
        return symbols.get("subtitles_off");
    }

    public static MaterialSymbol iconSubway() {
        return symbols.get("subway");
    }

    public static MaterialSymbol iconSubwayWalk() {
        return symbols.get("subway_walk");
    }

    public static MaterialSymbol iconSummarize() {
        return symbols.get("summarize");
    }

    public static MaterialSymbol iconSunny() {
        return symbols.get("sunny");
    }

    public static MaterialSymbol iconSunnySnowing() {
        return symbols.get("sunny_snowing");
    }

    public static MaterialSymbol iconSuperscript() {
        return symbols.get("superscript");
    }

    public static MaterialSymbol iconSupervisedUserCircle() {
        return symbols.get("supervised_user_circle");
    }

    public static MaterialSymbol iconSupervisedUserCircleOff() {
        return symbols.get("supervised_user_circle_off");
    }

    public static MaterialSymbol iconSupervisorAccount() {
        return symbols.get("supervisor_account");
    }

    public static MaterialSymbol iconSupport() {
        return symbols.get("support");
    }

    public static MaterialSymbol iconSupportAgent() {
        return symbols.get("support_agent");
    }

    public static MaterialSymbol iconSurfing() {
        return symbols.get("surfing");
    }

    public static MaterialSymbol iconSurgical() {
        return symbols.get("surgical");
    }

    public static MaterialSymbol iconSurroundSound() {
        return symbols.get("surround_sound");
    }

    public static MaterialSymbol iconSwapCalls() {
        return symbols.get("swap_calls");
    }

    public static MaterialSymbol iconSwapDrivingApps() {
        return symbols.get("swap_driving_apps");
    }

    public static MaterialSymbol iconSwapDrivingAppsWheel() {
        return symbols.get("swap_driving_apps_wheel");
    }

    public static MaterialSymbol iconSwapHoriz() {
        return symbols.get("swap_horiz");
    }

    public static MaterialSymbol iconSwapHorizontalCircle() {
        return symbols.get("swap_horizontal_circle");
    }

    public static MaterialSymbol iconSwapVert() {
        return symbols.get("swap_vert");
    }

    public static MaterialSymbol iconSwapVerticalCircle() {
        return symbols.get("swap_vertical_circle");
    }

    public static MaterialSymbol iconSweep() {
        return symbols.get("sweep");
    }

    public static MaterialSymbol iconSwipe() {
        return symbols.get("swipe");
    }

    public static MaterialSymbol iconSwipeDown() {
        return symbols.get("swipe_down");
    }

    public static MaterialSymbol iconSwipeDownAlt() {
        return symbols.get("swipe_down_alt");
    }

    public static MaterialSymbol iconSwipeLeft() {
        return symbols.get("swipe_left");
    }

    public static MaterialSymbol iconSwipeLeftAlt() {
        return symbols.get("swipe_left_alt");
    }

    public static MaterialSymbol iconSwipeRight() {
        return symbols.get("swipe_right");
    }

    public static MaterialSymbol iconSwipeRightAlt() {
        return symbols.get("swipe_right_alt");
    }

    public static MaterialSymbol iconSwipeUp() {
        return symbols.get("swipe_up");
    }

    public static MaterialSymbol iconSwipeUpAlt() {
        return symbols.get("swipe_up_alt");
    }

    public static MaterialSymbol iconSwipeVertical() {
        return symbols.get("swipe_vertical");
    }

    public static MaterialSymbol iconSwitch() {
        return symbols.get("switch");
    }

    public static MaterialSymbol iconSwitchAccess() {
        return symbols.get("switch_access");
    }

    public static MaterialSymbol iconSwitchAccess2() {
        return symbols.get("switch_access_2");
    }

    public static MaterialSymbol iconSwitchAccess3() {
        return symbols.get("switch_access_3");
    }

    public static MaterialSymbol iconSwitchAccessShortcut() {
        return symbols.get("switch_access_shortcut");
    }

    public static MaterialSymbol iconSwitchAccessShortcutAdd() {
        return symbols.get("switch_access_shortcut_add");
    }

    public static MaterialSymbol iconSwitchAccount() {
        return symbols.get("switch_account");
    }

    public static MaterialSymbol iconSwitchCamera() {
        return symbols.get("switch_camera");
    }

    public static MaterialSymbol iconSwitchLeft() {
        return symbols.get("switch_left");
    }

    public static MaterialSymbol iconSwitchRight() {
        return symbols.get("switch_right");
    }

    public static MaterialSymbol iconSwitchVideo() {
        return symbols.get("switch_video");
    }

    public static MaterialSymbol iconSwitches() {
        return symbols.get("switches");
    }

    public static MaterialSymbol iconSwordRose() {
        return symbols.get("sword_rose");
    }

    public static MaterialSymbol iconSwords() {
        return symbols.get("swords");
    }

    public static MaterialSymbol iconSymptoms() {
        return symbols.get("symptoms");
    }

    public static MaterialSymbol iconSynagogue() {
        return symbols.get("synagogue");
    }

    public static MaterialSymbol iconSync() {
        return symbols.get("sync");
    }

    public static MaterialSymbol iconSyncAlt() {
        return symbols.get("sync_alt");
    }

    public static MaterialSymbol iconSyncArrowDown() {
        return symbols.get("sync_arrow_down");
    }

    public static MaterialSymbol iconSyncArrowUp() {
        return symbols.get("sync_arrow_up");
    }

    public static MaterialSymbol iconSyncDesktop() {
        return symbols.get("sync_desktop");
    }

    public static MaterialSymbol iconSyncDisabled() {
        return symbols.get("sync_disabled");
    }

    public static MaterialSymbol iconSyncLock() {
        return symbols.get("sync_lock");
    }

    public static MaterialSymbol iconSyncProblem() {
        return symbols.get("sync_problem");
    }

    public static MaterialSymbol iconSyncSavedLocally() {
        return symbols.get("sync_saved_locally");
    }

    public static MaterialSymbol iconSyncSavedLocallyOff() {
        return symbols.get("sync_saved_locally_off");
    }

    public static MaterialSymbol iconSyringe() {
        return symbols.get("syringe");
    }

    public static MaterialSymbol iconSystemSecurityUpdate() {
        return symbols.get("system_security_update");
    }

    public static MaterialSymbol iconSystemSecurityUpdateGood() {
        return symbols.get("system_security_update_good");
    }

    public static MaterialSymbol iconSystemSecurityUpdateWarning() {
        return symbols.get("system_security_update_warning");
    }

    public static MaterialSymbol iconSystemUpdate() {
        return symbols.get("system_update");
    }

    public static MaterialSymbol iconSystemUpdateAlt() {
        return symbols.get("system_update_alt");
    }

    public static MaterialSymbol iconTab() {
        return symbols.get("tab");
    }

    public static MaterialSymbol iconTabClose() {
        return symbols.get("tab_close");
    }

    public static MaterialSymbol iconTabCloseInactive() {
        return symbols.get("tab_close_inactive");
    }

    public static MaterialSymbol iconTabCloseRight() {
        return symbols.get("tab_close_right");
    }

    public static MaterialSymbol iconTabDuplicate() {
        return symbols.get("tab_duplicate");
    }

    public static MaterialSymbol iconTabGroup() {
        return symbols.get("tab_group");
    }

    public static MaterialSymbol iconTabInactive() {
        return symbols.get("tab_inactive");
    }

    public static MaterialSymbol iconTabMove() {
        return symbols.get("tab_move");
    }

    public static MaterialSymbol iconTabNewRight() {
        return symbols.get("tab_new_right");
    }

    public static MaterialSymbol iconTabRecent() {
        return symbols.get("tab_recent");
    }

    public static MaterialSymbol iconTabSearch() {
        return symbols.get("tab_search");
    }

    public static MaterialSymbol iconTabUnselected() {
        return symbols.get("tab_unselected");
    }

    public static MaterialSymbol iconTable() {
        return symbols.get("table");
    }

    public static MaterialSymbol iconTableBar() {
        return symbols.get("table_bar");
    }

    public static MaterialSymbol iconTableChart() {
        return symbols.get("table_chart");
    }

    public static MaterialSymbol iconTableChartView() {
        return symbols.get("table_chart_view");
    }

    public static MaterialSymbol iconTableConvert() {
        return symbols.get("table_convert");
    }

    public static MaterialSymbol iconTableEdit() {
        return symbols.get("table_edit");
    }

    public static MaterialSymbol iconTableEye() {
        return symbols.get("table_eye");
    }

    public static MaterialSymbol iconTableLamp() {
        return symbols.get("table_lamp");
    }

    public static MaterialSymbol iconTableLarge() {
        return symbols.get("table_large");
    }

    public static MaterialSymbol iconTableRestaurant() {
        return symbols.get("table_restaurant");
    }

    public static MaterialSymbol iconTableRows() {
        return symbols.get("table_rows");
    }

    public static MaterialSymbol iconTableRowsNarrow() {
        return symbols.get("table_rows_narrow");
    }

    public static MaterialSymbol iconTableSign() {
        return symbols.get("table_sign");
    }

    public static MaterialSymbol iconTableView() {
        return symbols.get("table_view");
    }

    public static MaterialSymbol iconTablet() {
        return symbols.get("tablet");
    }

    public static MaterialSymbol iconTabletAndroid() {
        return symbols.get("tablet_android");
    }

    public static MaterialSymbol iconTabletCamera() {
        return symbols.get("tablet_camera");
    }

    public static MaterialSymbol iconTabletMac() {
        return symbols.get("tablet_mac");
    }

    public static MaterialSymbol iconTabs() {
        return symbols.get("tabs");
    }

    public static MaterialSymbol iconTactic() {
        return symbols.get("tactic");
    }

    public static MaterialSymbol iconTag() {
        return symbols.get("tag");
    }

    public static MaterialSymbol iconTagFaces() {
        return symbols.get("tag_faces");
    }

    public static MaterialSymbol iconTakeoutDining() {
        return symbols.get("takeout_dining");
    }

    public static MaterialSymbol iconTakeoutDining2() {
        return symbols.get("takeout_dining_2");
    }

    public static MaterialSymbol iconTamperDetectionOff() {
        return symbols.get("tamper_detection_off");
    }

    public static MaterialSymbol iconTamperDetectionOn() {
        return symbols.get("tamper_detection_on");
    }

    public static MaterialSymbol iconTapAndPlay() {
        return symbols.get("tap_and_play");
    }

    public static MaterialSymbol iconTapas() {
        return symbols.get("tapas");
    }

    public static MaterialSymbol iconTarget() {
        return symbols.get("target");
    }

    public static MaterialSymbol iconTask() {
        return symbols.get("task");
    }

    public static MaterialSymbol iconTaskAlt() {
        return symbols.get("task_alt");
    }

    public static MaterialSymbol iconTatamiSeat() {
        return symbols.get("tatami_seat");
    }

    public static MaterialSymbol iconTaunt() {
        return symbols.get("taunt");
    }

    public static MaterialSymbol iconTaxiAlert() {
        return symbols.get("taxi_alert");
    }

    public static MaterialSymbol iconTeamDashboard() {
        return symbols.get("team_dashboard");
    }

    public static MaterialSymbol iconTempPreferencesCustom() {
        return symbols.get("temp_preferences_custom");
    }

    public static MaterialSymbol iconTempPreferencesEco() {
        return symbols.get("temp_preferences_eco");
    }

    public static MaterialSymbol iconTempleBuddhist() {
        return symbols.get("temple_buddhist");
    }

    public static MaterialSymbol iconTempleHindu() {
        return symbols.get("temple_hindu");
    }

    public static MaterialSymbol iconTenancy() {
        return symbols.get("tenancy");
    }

    public static MaterialSymbol iconTerminal() {
        return symbols.get("terminal");
    }

    public static MaterialSymbol iconTerrain() {
        return symbols.get("terrain");
    }

    public static MaterialSymbol iconTextAd() {
        return symbols.get("text_ad");
    }

    public static MaterialSymbol iconTextCompare() {
        return symbols.get("text_compare");
    }

    public static MaterialSymbol iconTextDecrease() {
        return symbols.get("text_decrease");
    }

    public static MaterialSymbol iconTextFields() {
        return symbols.get("text_fields");
    }

    public static MaterialSymbol iconTextFieldsAlt() {
        return symbols.get("text_fields_alt");
    }

    public static MaterialSymbol iconTextFormat() {
        return symbols.get("text_format");
    }

    public static MaterialSymbol iconTextIncrease() {
        return symbols.get("text_increase");
    }

    public static MaterialSymbol iconTextRotateUp() {
        return symbols.get("text_rotate_up");
    }

    public static MaterialSymbol iconTextRotateVertical() {
        return symbols.get("text_rotate_vertical");
    }

    public static MaterialSymbol iconTextRotationAngledown() {
        return symbols.get("text_rotation_angledown");
    }

    public static MaterialSymbol iconTextRotationAngleup() {
        return symbols.get("text_rotation_angleup");
    }

    public static MaterialSymbol iconTextRotationDown() {
        return symbols.get("text_rotation_down");
    }

    public static MaterialSymbol iconTextRotationNone() {
        return symbols.get("text_rotation_none");
    }

    public static MaterialSymbol iconTextSelectEnd() {
        return symbols.get("text_select_end");
    }

    public static MaterialSymbol iconTextSelectJumpToBeginning() {
        return symbols.get("text_select_jump_to_beginning");
    }

    public static MaterialSymbol iconTextSelectJumpToEnd() {
        return symbols.get("text_select_jump_to_end");
    }

    public static MaterialSymbol iconTextSelectMoveBackCharacter() {
        return symbols.get("text_select_move_back_character");
    }

    public static MaterialSymbol iconTextSelectMoveBackWord() {
        return symbols.get("text_select_move_back_word");
    }

    public static MaterialSymbol iconTextSelectMoveDown() {
        return symbols.get("text_select_move_down");
    }

    public static MaterialSymbol iconTextSelectMoveForwardCharacter() {
        return symbols.get("text_select_move_forward_character");
    }

    public static MaterialSymbol iconTextSelectMoveForwardWord() {
        return symbols.get("text_select_move_forward_word");
    }

    public static MaterialSymbol iconTextSelectMoveUp() {
        return symbols.get("text_select_move_up");
    }

    public static MaterialSymbol iconTextSelectStart() {
        return symbols.get("text_select_start");
    }

    public static MaterialSymbol iconTextSnippet() {
        return symbols.get("text_snippet");
    }

    public static MaterialSymbol iconTextToSpeech() {
        return symbols.get("text_to_speech");
    }

    public static MaterialSymbol iconTextUp() {
        return symbols.get("text_up");
    }

    public static MaterialSymbol iconTextsms() {
        return symbols.get("textsms");
    }

    public static MaterialSymbol iconTexture() {
        return symbols.get("texture");
    }

    public static MaterialSymbol iconTextureAdd() {
        return symbols.get("texture_add");
    }

    public static MaterialSymbol iconTextureMinus() {
        return symbols.get("texture_minus");
    }

    public static MaterialSymbol iconTheaterComedy() {
        return symbols.get("theater_comedy");
    }

    public static MaterialSymbol iconTheaters() {
        return symbols.get("theaters");
    }

    public static MaterialSymbol iconThermometer() {
        return symbols.get("thermometer");
    }

    public static MaterialSymbol iconThermometerAdd() {
        return symbols.get("thermometer_add");
    }

    public static MaterialSymbol iconThermometerGain() {
        return symbols.get("thermometer_gain");
    }

    public static MaterialSymbol iconThermometerLoss() {
        return symbols.get("thermometer_loss");
    }

    public static MaterialSymbol iconThermometerMinus() {
        return symbols.get("thermometer_minus");
    }

    public static MaterialSymbol iconThermostat() {
        return symbols.get("thermostat");
    }

    public static MaterialSymbol iconThermostatArrowDown() {
        return symbols.get("thermostat_arrow_down");
    }

    public static MaterialSymbol iconThermostatArrowUp() {
        return symbols.get("thermostat_arrow_up");
    }

    public static MaterialSymbol iconThermostatAuto() {
        return symbols.get("thermostat_auto");
    }

    public static MaterialSymbol iconThermostatCarbon() {
        return symbols.get("thermostat_carbon");
    }

    public static MaterialSymbol iconThingsToDo() {
        return symbols.get("things_to_do");
    }

    public static MaterialSymbol iconThreadUnread() {
        return symbols.get("thread_unread");
    }

    public static MaterialSymbol iconThreatIntelligence() {
        return symbols.get("threat_intelligence");
    }

    public static MaterialSymbol iconThumbDown() {
        return symbols.get("thumb_down");
    }

    public static MaterialSymbol iconThumbDownAlt() {
        return symbols.get("thumb_down_alt");
    }

    public static MaterialSymbol iconThumbDownFilled() {
        return symbols.get("thumb_down_filled");
    }

    public static MaterialSymbol iconThumbDownOff() {
        return symbols.get("thumb_down_off");
    }

    public static MaterialSymbol iconThumbDownOffAlt() {
        return symbols.get("thumb_down_off_alt");
    }

    public static MaterialSymbol iconThumbUp() {
        return symbols.get("thumb_up");
    }

    public static MaterialSymbol iconThumbUpAlt() {
        return symbols.get("thumb_up_alt");
    }

    public static MaterialSymbol iconThumbUpFilled() {
        return symbols.get("thumb_up_filled");
    }

    public static MaterialSymbol iconThumbUpOff() {
        return symbols.get("thumb_up_off");
    }

    public static MaterialSymbol iconThumbUpOffAlt() {
        return symbols.get("thumb_up_off_alt");
    }

    public static MaterialSymbol iconThumbnailBar() {
        return symbols.get("thumbnail_bar");
    }

    public static MaterialSymbol iconThumbsUpDouble() {
        return symbols.get("thumbs_up_double");
    }

    public static MaterialSymbol iconThumbsUpDown() {
        return symbols.get("thumbs_up_down");
    }

    public static MaterialSymbol iconThunderstorm() {
        return symbols.get("thunderstorm");
    }

    public static MaterialSymbol iconTibia() {
        return symbols.get("tibia");
    }

    public static MaterialSymbol iconTibiaAlt() {
        return symbols.get("tibia_alt");
    }

    public static MaterialSymbol iconTileLarge() {
        return symbols.get("tile_large");
    }

    public static MaterialSymbol iconTileMedium() {
        return symbols.get("tile_medium");
    }

    public static MaterialSymbol iconTileSmall() {
        return symbols.get("tile_small");
    }

    public static MaterialSymbol iconTimeAuto() {
        return symbols.get("time_auto");
    }

    public static MaterialSymbol iconTimeToLeave() {
        return symbols.get("time_to_leave");
    }

    public static MaterialSymbol iconTimelapse() {
        return symbols.get("timelapse");
    }

    public static MaterialSymbol iconTimeline() {
        return symbols.get("timeline");
    }

    public static MaterialSymbol iconTimer() {
        return symbols.get("timer");
    }

    public static MaterialSymbol iconTimer1() {
        return symbols.get("timer_1");
    }

    public static MaterialSymbol iconTimer10() {
        return symbols.get("timer_10");
    }

    public static MaterialSymbol iconTimer10Alt1() {
        return symbols.get("timer_10_alt_1");
    }

    public static MaterialSymbol iconTimer10Select() {
        return symbols.get("timer_10_select");
    }

    public static MaterialSymbol iconTimer2() {
        return symbols.get("timer_2");
    }

    public static MaterialSymbol iconTimer3() {
        return symbols.get("timer_3");
    }

    public static MaterialSymbol iconTimer3Alt1() {
        return symbols.get("timer_3_alt_1");
    }

    public static MaterialSymbol iconTimer3Select() {
        return symbols.get("timer_3_select");
    }

    public static MaterialSymbol iconTimer5() {
        return symbols.get("timer_5");
    }

    public static MaterialSymbol iconTimer5Shutter() {
        return symbols.get("timer_5_shutter");
    }

    public static MaterialSymbol iconTimerArrowDown() {
        return symbols.get("timer_arrow_down");
    }

    public static MaterialSymbol iconTimerArrowUp() {
        return symbols.get("timer_arrow_up");
    }

    public static MaterialSymbol iconTimerOff() {
        return symbols.get("timer_off");
    }

    public static MaterialSymbol iconTimerPause() {
        return symbols.get("timer_pause");
    }

    public static MaterialSymbol iconTimerPlay() {
        return symbols.get("timer_play");
    }

    public static MaterialSymbol iconTipsAndUpdates() {
        return symbols.get("tips_and_updates");
    }

    public static MaterialSymbol iconTireRepair() {
        return symbols.get("tire_repair");
    }

    public static MaterialSymbol iconTitle() {
        return symbols.get("title");
    }

    public static MaterialSymbol iconTitlecase() {
        return symbols.get("titlecase");
    }

    public static MaterialSymbol iconToast() {
        return symbols.get("toast");
    }

    public static MaterialSymbol iconToc() {
        return symbols.get("toc");
    }

    public static MaterialSymbol iconToday() {
        return symbols.get("today");
    }

    public static MaterialSymbol iconToggleOff() {
        return symbols.get("toggle_off");
    }

    public static MaterialSymbol iconToggleOn() {
        return symbols.get("toggle_on");
    }

    public static MaterialSymbol iconToken() {
        return symbols.get("token");
    }

    public static MaterialSymbol iconToll() {
        return symbols.get("toll");
    }

    public static MaterialSymbol iconTonality() {
        return symbols.get("tonality");
    }

    public static MaterialSymbol iconTonality2() {
        return symbols.get("tonality_2");
    }

    public static MaterialSymbol iconToolbar() {
        return symbols.get("toolbar");
    }

    public static MaterialSymbol iconToolsFlatHead() {
        return symbols.get("tools_flat_head");
    }

    public static MaterialSymbol iconToolsInstallationKit() {
        return symbols.get("tools_installation_kit");
    }

    public static MaterialSymbol iconToolsLadder() {
        return symbols.get("tools_ladder");
    }

    public static MaterialSymbol iconToolsLevel() {
        return symbols.get("tools_level");
    }

    public static MaterialSymbol iconToolsPhillips() {
        return symbols.get("tools_phillips");
    }

    public static MaterialSymbol iconToolsPliersWireStripper() {
        return symbols.get("tools_pliers_wire_stripper");
    }

    public static MaterialSymbol iconToolsPowerDrill() {
        return symbols.get("tools_power_drill");
    }

    public static MaterialSymbol iconToolsWrench() {
        return symbols.get("tools_wrench");
    }

    public static MaterialSymbol iconTooltip() {
        return symbols.get("tooltip");
    }

    public static MaterialSymbol iconTooltip2() {
        return symbols.get("tooltip_2");
    }

    public static MaterialSymbol iconTopPanelClose() {
        return symbols.get("top_panel_close");
    }

    public static MaterialSymbol iconTopPanelOpen() {
        return symbols.get("top_panel_open");
    }

    public static MaterialSymbol iconTopic() {
        return symbols.get("topic");
    }

    public static MaterialSymbol iconTornado() {
        return symbols.get("tornado");
    }

    public static MaterialSymbol iconTotalDissolvedSolids() {
        return symbols.get("total_dissolved_solids");
    }

    public static MaterialSymbol iconTouchApp() {
        return symbols.get("touch_app");
    }

    public static MaterialSymbol iconTouchDouble() {
        return symbols.get("touch_double");
    }

    public static MaterialSymbol iconTouchLong() {
        return symbols.get("touch_long");
    }

    public static MaterialSymbol iconTouchTriple() {
        return symbols.get("touch_triple");
    }

    public static MaterialSymbol iconTouchpadMouse() {
        return symbols.get("touchpad_mouse");
    }

    public static MaterialSymbol iconTouchpadMouseOff() {
        return symbols.get("touchpad_mouse_off");
    }

    public static MaterialSymbol iconTour() {
        return symbols.get("tour");
    }

    public static MaterialSymbol iconToys() {
        return symbols.get("toys");
    }

    public static MaterialSymbol iconToysAndGames() {
        return symbols.get("toys_and_games");
    }

    public static MaterialSymbol iconToysFan() {
        return symbols.get("toys_fan");
    }

    public static MaterialSymbol iconTrackChanges() {
        return symbols.get("track_changes");
    }

    public static MaterialSymbol iconTrackpadInput() {
        return symbols.get("trackpad_input");
    }

    public static MaterialSymbol iconTrackpadInput2() {
        return symbols.get("trackpad_input_2");
    }

    public static MaterialSymbol iconTrackpadInput3() {
        return symbols.get("trackpad_input_3");
    }

    public static MaterialSymbol iconTraffic() {
        return symbols.get("traffic");
    }

    public static MaterialSymbol iconTrafficJam() {
        return symbols.get("traffic_jam");
    }

    public static MaterialSymbol iconTrailLength() {
        return symbols.get("trail_length");
    }

    public static MaterialSymbol iconTrailLengthMedium() {
        return symbols.get("trail_length_medium");
    }

    public static MaterialSymbol iconTrailLengthShort() {
        return symbols.get("trail_length_short");
    }

    public static MaterialSymbol iconTrain() {
        return symbols.get("train");
    }

    public static MaterialSymbol iconTram() {
        return symbols.get("tram");
    }

    public static MaterialSymbol iconTranscribe() {
        return symbols.get("transcribe");
    }

    public static MaterialSymbol iconTransferWithinAStation() {
        return symbols.get("transfer_within_a_station");
    }

    public static MaterialSymbol iconTransform() {
        return symbols.get("transform");
    }

    public static MaterialSymbol iconTransgender() {
        return symbols.get("transgender");
    }

    public static MaterialSymbol iconTransitEnterexit() {
        return symbols.get("transit_enterexit");
    }

    public static MaterialSymbol iconTransitTicket() {
        return symbols.get("transit_ticket");
    }

    public static MaterialSymbol iconTransitionChop() {
        return symbols.get("transition_chop");
    }

    public static MaterialSymbol iconTransitionDissolve() {
        return symbols.get("transition_dissolve");
    }

    public static MaterialSymbol iconTransitionFade() {
        return symbols.get("transition_fade");
    }

    public static MaterialSymbol iconTransitionPush() {
        return symbols.get("transition_push");
    }

    public static MaterialSymbol iconTransitionSlide() {
        return symbols.get("transition_slide");
    }

    public static MaterialSymbol iconTranslate() {
        return symbols.get("translate");
    }

    public static MaterialSymbol iconTranslateIndic() {
        return symbols.get("translate_indic");
    }

    public static MaterialSymbol iconTransportation() {
        return symbols.get("transportation");
    }

    public static MaterialSymbol iconTravel() {
        return symbols.get("travel");
    }

    public static MaterialSymbol iconTravelExplore() {
        return symbols.get("travel_explore");
    }

    public static MaterialSymbol iconTravelLuggageAndBags() {
        return symbols.get("travel_luggage_and_bags");
    }

    public static MaterialSymbol iconTrendingDown() {
        return symbols.get("trending_down");
    }

    public static MaterialSymbol iconTrendingFlat() {
        return symbols.get("trending_flat");
    }

    public static MaterialSymbol iconTrendingUp() {
        return symbols.get("trending_up");
    }

    public static MaterialSymbol iconTrip() {
        return symbols.get("trip");
    }

    public static MaterialSymbol iconTripOrigin() {
        return symbols.get("trip_origin");
    }

    public static MaterialSymbol iconTrolley() {
        return symbols.get("trolley");
    }

    public static MaterialSymbol iconTrolleyCableCar() {
        return symbols.get("trolley_cable_car");
    }

    public static MaterialSymbol iconTrophy() {
        return symbols.get("trophy");
    }

    public static MaterialSymbol iconTroubleshoot() {
        return symbols.get("troubleshoot");
    }

    public static MaterialSymbol iconTry() {
        return symbols.get("try");
    }

    public static MaterialSymbol iconTsunami() {
        return symbols.get("tsunami");
    }

    public static MaterialSymbol iconTsv() {
        return symbols.get("tsv");
    }

    public static MaterialSymbol iconTty() {
        return symbols.get("tty");
    }

    public static MaterialSymbol iconTune() {
        return symbols.get("tune");
    }

    public static MaterialSymbol iconTungsten() {
        return symbols.get("tungsten");
    }

    public static MaterialSymbol iconTurnLeft() {
        return symbols.get("turn_left");
    }

    public static MaterialSymbol iconTurnRight() {
        return symbols.get("turn_right");
    }

    public static MaterialSymbol iconTurnSharpLeft() {
        return symbols.get("turn_sharp_left");
    }

    public static MaterialSymbol iconTurnSharpRight() {
        return symbols.get("turn_sharp_right");
    }

    public static MaterialSymbol iconTurnSlightLeft() {
        return symbols.get("turn_slight_left");
    }

    public static MaterialSymbol iconTurnSlightRight() {
        return symbols.get("turn_slight_right");
    }

    public static MaterialSymbol iconTurnedIn() {
        return symbols.get("turned_in");
    }

    public static MaterialSymbol iconTurnedInNot() {
        return symbols.get("turned_in_not");
    }

    public static MaterialSymbol iconTv() {
        return symbols.get("tv");
    }

    public static MaterialSymbol iconTvDisplays() {
        return symbols.get("tv_displays");
    }

    public static MaterialSymbol iconTvGen() {
        return symbols.get("tv_gen");
    }

    public static MaterialSymbol iconTvGuide() {
        return symbols.get("tv_guide");
    }

    public static MaterialSymbol iconTvNext() {
        return symbols.get("tv_next");
    }

    public static MaterialSymbol iconTvOff() {
        return symbols.get("tv_off");
    }

    public static MaterialSymbol iconTvOptionsEditChannels() {
        return symbols.get("tv_options_edit_channels");
    }

    public static MaterialSymbol iconTvOptionsInputSettings() {
        return symbols.get("tv_options_input_settings");
    }

    public static MaterialSymbol iconTvRemote() {
        return symbols.get("tv_remote");
    }

    public static MaterialSymbol iconTvSignin() {
        return symbols.get("tv_signin");
    }

    public static MaterialSymbol iconTvWithAssistant() {
        return symbols.get("tv_with_assistant");
    }

    public static MaterialSymbol iconTwoPager() {
        return symbols.get("two_pager");
    }

    public static MaterialSymbol iconTwoPagerStore() {
        return symbols.get("two_pager_store");
    }

    public static MaterialSymbol iconTwoWheeler() {
        return symbols.get("two_wheeler");
    }

    public static MaterialSymbol iconTypeSpecimen() {
        return symbols.get("type_specimen");
    }

    public static MaterialSymbol iconUTurnLeft() {
        return symbols.get("u_turn_left");
    }

    public static MaterialSymbol iconUTurnRight() {
        return symbols.get("u_turn_right");
    }

    public static MaterialSymbol iconUdon() {
        return symbols.get("udon");
    }

    public static MaterialSymbol iconUlnaRadius() {
        return symbols.get("ulna_radius");
    }

    public static MaterialSymbol iconUlnaRadiusAlt() {
        return symbols.get("ulna_radius_alt");
    }

    public static MaterialSymbol iconUmbrella() {
        return symbols.get("umbrella");
    }

    public static MaterialSymbol iconUnarchive() {
        return symbols.get("unarchive");
    }

    public static MaterialSymbol iconUndo() {
        return symbols.get("undo");
    }

    public static MaterialSymbol iconUnfoldLess() {
        return symbols.get("unfold_less");
    }

    public static MaterialSymbol iconUnfoldLessDouble() {
        return symbols.get("unfold_less_double");
    }

    public static MaterialSymbol iconUnfoldMore() {
        return symbols.get("unfold_more");
    }

    public static MaterialSymbol iconUnfoldMoreDouble() {
        return symbols.get("unfold_more_double");
    }

    public static MaterialSymbol iconUngroup() {
        return symbols.get("ungroup");
    }

    public static MaterialSymbol iconUniversalCurrency() {
        return symbols.get("universal_currency");
    }

    public static MaterialSymbol iconUniversalCurrencyAlt() {
        return symbols.get("universal_currency_alt");
    }

    public static MaterialSymbol iconUniversalLocal() {
        return symbols.get("universal_local");
    }

    public static MaterialSymbol iconUnknown2() {
        return symbols.get("unknown_2");
    }

    public static MaterialSymbol iconUnknown5() {
        return symbols.get("unknown_5");
    }

    public static MaterialSymbol iconUnknown7() {
        return symbols.get("unknown_7");
    }

    public static MaterialSymbol iconUnknownDocument() {
        return symbols.get("unknown_document");
    }

    public static MaterialSymbol iconUnknownMed() {
        return symbols.get("unknown_med");
    }

    public static MaterialSymbol iconUnlicense() {
        return symbols.get("unlicense");
    }

    public static MaterialSymbol iconUnpavedRoad() {
        return symbols.get("unpaved_road");
    }

    public static MaterialSymbol iconUnpin() {
        return symbols.get("unpin");
    }

    public static MaterialSymbol iconUnpublished() {
        return symbols.get("unpublished");
    }

    public static MaterialSymbol iconUnsubscribe() {
        return symbols.get("unsubscribe");
    }

    public static MaterialSymbol iconUpcoming() {
        return symbols.get("upcoming");
    }

    public static MaterialSymbol iconUpdate() {
        return symbols.get("update");
    }

    public static MaterialSymbol iconUpdateDisabled() {
        return symbols.get("update_disabled");
    }

    public static MaterialSymbol iconUpgrade() {
        return symbols.get("upgrade");
    }

    public static MaterialSymbol iconUpiPay() {
        return symbols.get("upi_pay");
    }

    public static MaterialSymbol iconUpload() {
        return symbols.get("upload");
    }

    public static MaterialSymbol iconUpload2() {
        return symbols.get("upload_2");
    }

    public static MaterialSymbol iconUploadFile() {
        return symbols.get("upload_file");
    }

    public static MaterialSymbol iconUppercase() {
        return symbols.get("uppercase");
    }

    public static MaterialSymbol iconUrology() {
        return symbols.get("urology");
    }

    public static MaterialSymbol iconUsb() {
        return symbols.get("usb");
    }

    public static MaterialSymbol iconUsbOff() {
        return symbols.get("usb_off");
    }

    public static MaterialSymbol iconUserAttributes() {
        return symbols.get("user_attributes");
    }

    public static MaterialSymbol iconVaccines() {
        return symbols.get("vaccines");
    }

    public static MaterialSymbol iconVacuum() {
        return symbols.get("vacuum");
    }

    public static MaterialSymbol iconValve() {
        return symbols.get("valve");
    }

    public static MaterialSymbol iconVapeFree() {
        return symbols.get("vape_free");
    }

    public static MaterialSymbol iconVapingRooms() {
        return symbols.get("vaping_rooms");
    }

    public static MaterialSymbol iconVariableAdd() {
        return symbols.get("variable_add");
    }

    public static MaterialSymbol iconVariableInsert() {
        return symbols.get("variable_insert");
    }

    public static MaterialSymbol iconVariableRemove() {
        return symbols.get("variable_remove");
    }

    public static MaterialSymbol iconVariables() {
        return symbols.get("variables");
    }

    public static MaterialSymbol iconVentilator() {
        return symbols.get("ventilator");
    }

    public static MaterialSymbol iconVerified() {
        return symbols.get("verified");
    }

    public static MaterialSymbol iconVerifiedOff() {
        return symbols.get("verified_off");
    }

    public static MaterialSymbol iconVerifiedUser() {
        return symbols.get("verified_user");
    }

    public static MaterialSymbol iconVerticalAlignBottom() {
        return symbols.get("vertical_align_bottom");
    }

    public static MaterialSymbol iconVerticalAlignCenter() {
        return symbols.get("vertical_align_center");
    }

    public static MaterialSymbol iconVerticalAlignTop() {
        return symbols.get("vertical_align_top");
    }

    public static MaterialSymbol iconVerticalDistribute() {
        return symbols.get("vertical_distribute");
    }

    public static MaterialSymbol iconVerticalShades() {
        return symbols.get("vertical_shades");
    }

    public static MaterialSymbol iconVerticalShadesClosed() {
        return symbols.get("vertical_shades_closed");
    }

    public static MaterialSymbol iconVerticalSplit() {
        return symbols.get("vertical_split");
    }

    public static MaterialSymbol iconVibration() {
        return symbols.get("vibration");
    }

    public static MaterialSymbol iconVideoCall() {
        return symbols.get("video_call");
    }

    public static MaterialSymbol iconVideoCameraBack() {
        return symbols.get("video_camera_back");
    }

    public static MaterialSymbol iconVideoCameraBackAdd() {
        return symbols.get("video_camera_back_add");
    }

    public static MaterialSymbol iconVideoCameraFront() {
        return symbols.get("video_camera_front");
    }

    public static MaterialSymbol iconVideoCameraFrontOff() {
        return symbols.get("video_camera_front_off");
    }

    public static MaterialSymbol iconVideoChat() {
        return symbols.get("video_chat");
    }

    public static MaterialSymbol iconVideoFile() {
        return symbols.get("video_file");
    }

    public static MaterialSymbol iconVideoLabel() {
        return symbols.get("video_label");
    }

    public static MaterialSymbol iconVideoLibrary() {
        return symbols.get("video_library");
    }

    public static MaterialSymbol iconVideoSearch() {
        return symbols.get("video_search");
    }

    public static MaterialSymbol iconVideoSettings() {
        return symbols.get("video_settings");
    }

    public static MaterialSymbol iconVideoStable() {
        return symbols.get("video_stable");
    }

    public static MaterialSymbol iconVideocam() {
        return symbols.get("videocam");
    }

    public static MaterialSymbol iconVideocamAlert() {
        return symbols.get("videocam_alert");
    }

    public static MaterialSymbol iconVideocamOff() {
        return symbols.get("videocam_off");
    }

    public static MaterialSymbol iconVideogameAsset() {
        return symbols.get("videogame_asset");
    }

    public static MaterialSymbol iconVideogameAssetOff() {
        return symbols.get("videogame_asset_off");
    }

    public static MaterialSymbol iconViewAgenda() {
        return symbols.get("view_agenda");
    }

    public static MaterialSymbol iconViewApps() {
        return symbols.get("view_apps");
    }

    public static MaterialSymbol iconViewArray() {
        return symbols.get("view_array");
    }

    public static MaterialSymbol iconViewCarousel() {
        return symbols.get("view_carousel");
    }

    public static MaterialSymbol iconViewColumn() {
        return symbols.get("view_column");
    }

    public static MaterialSymbol iconViewColumn2() {
        return symbols.get("view_column_2");
    }

    public static MaterialSymbol iconViewComfy() {
        return symbols.get("view_comfy");
    }

    public static MaterialSymbol iconViewComfyAlt() {
        return symbols.get("view_comfy_alt");
    }

    public static MaterialSymbol iconViewCompact() {
        return symbols.get("view_compact");
    }

    public static MaterialSymbol iconViewCompactAlt() {
        return symbols.get("view_compact_alt");
    }

    public static MaterialSymbol iconViewCozy() {
        return symbols.get("view_cozy");
    }

    public static MaterialSymbol iconViewDay() {
        return symbols.get("view_day");
    }

    public static MaterialSymbol iconViewHeadline() {
        return symbols.get("view_headline");
    }

    public static MaterialSymbol iconViewInAr() {
        return symbols.get("view_in_ar");
    }

    public static MaterialSymbol iconViewInArNew() {
        return symbols.get("view_in_ar_new");
    }

    public static MaterialSymbol iconViewInArOff() {
        return symbols.get("view_in_ar_off");
    }

    public static MaterialSymbol iconViewKanban() {
        return symbols.get("view_kanban");
    }

    public static MaterialSymbol iconViewList() {
        return symbols.get("view_list");
    }

    public static MaterialSymbol iconViewModule() {
        return symbols.get("view_module");
    }

    public static MaterialSymbol iconViewObjectTrack() {
        return symbols.get("view_object_track");
    }

    public static MaterialSymbol iconViewQuilt() {
        return symbols.get("view_quilt");
    }

    public static MaterialSymbol iconViewRealSize() {
        return symbols.get("view_real_size");
    }

    public static MaterialSymbol iconViewSidebar() {
        return symbols.get("view_sidebar");
    }

    public static MaterialSymbol iconViewStream() {
        return symbols.get("view_stream");
    }

    public static MaterialSymbol iconViewTimeline() {
        return symbols.get("view_timeline");
    }

    public static MaterialSymbol iconViewWeek() {
        return symbols.get("view_week");
    }

    public static MaterialSymbol iconVignette() {
        return symbols.get("vignette");
    }

    public static MaterialSymbol iconVignette2() {
        return symbols.get("vignette_2");
    }

    public static MaterialSymbol iconVilla() {
        return symbols.get("villa");
    }

    public static MaterialSymbol iconVisibility() {
        return symbols.get("visibility");
    }

    public static MaterialSymbol iconVisibilityLock() {
        return symbols.get("visibility_lock");
    }

    public static MaterialSymbol iconVisibilityOff() {
        return symbols.get("visibility_off");
    }

    public static MaterialSymbol iconVitalSigns() {
        return symbols.get("vital_signs");
    }

    public static MaterialSymbol iconVitals() {
        return symbols.get("vitals");
    }

    public static MaterialSymbol iconVo2Max() {
        return symbols.get("vo2_max");
    }

    public static MaterialSymbol iconVoiceChat() {
        return symbols.get("voice_chat");
    }

    public static MaterialSymbol iconVoiceOverOff() {
        return symbols.get("voice_over_off");
    }

    public static MaterialSymbol iconVoiceSelection() {
        return symbols.get("voice_selection");
    }

    public static MaterialSymbol iconVoiceSelectionOff() {
        return symbols.get("voice_selection_off");
    }

    public static MaterialSymbol iconVoicemail() {
        return symbols.get("voicemail");
    }

    public static MaterialSymbol iconVoicemail2() {
        return symbols.get("voicemail_2");
    }

    public static MaterialSymbol iconVolcano() {
        return symbols.get("volcano");
    }

    public static MaterialSymbol iconVolumeDown() {
        return symbols.get("volume_down");
    }

    public static MaterialSymbol iconVolumeDownAlt() {
        return symbols.get("volume_down_alt");
    }

    public static MaterialSymbol iconVolumeMute() {
        return symbols.get("volume_mute");
    }

    public static MaterialSymbol iconVolumeOff() {
        return symbols.get("volume_off");
    }

    public static MaterialSymbol iconVolumeUp() {
        return symbols.get("volume_up");
    }

    public static MaterialSymbol iconVolunteerActivism() {
        return symbols.get("volunteer_activism");
    }

    public static MaterialSymbol iconVotingChip() {
        return symbols.get("voting_chip");
    }

    public static MaterialSymbol iconVpnKey() {
        return symbols.get("vpn_key");
    }

    public static MaterialSymbol iconVpnKeyAlert() {
        return symbols.get("vpn_key_alert");
    }

    public static MaterialSymbol iconVpnKeyOff() {
        return symbols.get("vpn_key_off");
    }

    public static MaterialSymbol iconVpnLock() {
        return symbols.get("vpn_lock");
    }

    public static MaterialSymbol iconVpnLock2() {
        return symbols.get("vpn_lock_2");
    }

    public static MaterialSymbol iconVr180Create2d() {
        return symbols.get("vr180_create2d");
    }

    public static MaterialSymbol iconVr180Create2dOff() {
        return symbols.get("vr180_create2d_off");
    }

    public static MaterialSymbol iconVrpano() {
        return symbols.get("vrpano");
    }

    public static MaterialSymbol iconWallArt() {
        return symbols.get("wall_art");
    }

    public static MaterialSymbol iconWallLamp() {
        return symbols.get("wall_lamp");
    }

    public static MaterialSymbol iconWallet() {
        return symbols.get("wallet");
    }

    public static MaterialSymbol iconWallpaper() {
        return symbols.get("wallpaper");
    }

    public static MaterialSymbol iconWallpaperSlideshow() {
        return symbols.get("wallpaper_slideshow");
    }

    public static MaterialSymbol iconWandShine() {
        return symbols.get("wand_shine");
    }

    public static MaterialSymbol iconWandStars() {
        return symbols.get("wand_stars");
    }

    public static MaterialSymbol iconWard() {
        return symbols.get("ward");
    }

    public static MaterialSymbol iconWarehouse() {
        return symbols.get("warehouse");
    }

    public static MaterialSymbol iconWarning() {
        return symbols.get("warning");
    }

    public static MaterialSymbol iconWarningAmber() {
        return symbols.get("warning_amber");
    }

    public static MaterialSymbol iconWarningOff() {
        return symbols.get("warning_off");
    }

    public static MaterialSymbol iconWash() {
        return symbols.get("wash");
    }

    public static MaterialSymbol iconWashoku() {
        return symbols.get("washoku");
    }

    public static MaterialSymbol iconWatch() {
        return symbols.get("watch");
    }

    public static MaterialSymbol iconWatchArrow() {
        return symbols.get("watch_arrow");
    }

    public static MaterialSymbol iconWatchButtonPress() {
        return symbols.get("watch_button_press");
    }

    public static MaterialSymbol iconWatchCheck() {
        return symbols.get("watch_check");
    }

    public static MaterialSymbol iconWatchLater() {
        return symbols.get("watch_later");
    }

    public static MaterialSymbol iconWatchOff() {
        return symbols.get("watch_off");
    }

    public static MaterialSymbol iconWatchScreentime() {
        return symbols.get("watch_screentime");
    }

    public static MaterialSymbol iconWatchVibration() {
        return symbols.get("watch_vibration");
    }

    public static MaterialSymbol iconWatchWake() {
        return symbols.get("watch_wake");
    }

    public static MaterialSymbol iconWater() {
        return symbols.get("water");
    }

    public static MaterialSymbol iconWaterBottle() {
        return symbols.get("water_bottle");
    }

    public static MaterialSymbol iconWaterBottleLarge() {
        return symbols.get("water_bottle_large");
    }

    public static MaterialSymbol iconWaterDamage() {
        return symbols.get("water_damage");
    }

    public static MaterialSymbol iconWaterDo() {
        return symbols.get("water_do");
    }

    public static MaterialSymbol iconWaterDrop() {
        return symbols.get("water_drop");
    }

    public static MaterialSymbol iconWaterEc() {
        return symbols.get("water_ec");
    }

    public static MaterialSymbol iconWaterFull() {
        return symbols.get("water_full");
    }

    public static MaterialSymbol iconWaterHeater() {
        return symbols.get("water_heater");
    }

    public static MaterialSymbol iconWaterLock() {
        return symbols.get("water_lock");
    }

    public static MaterialSymbol iconWaterLoss() {
        return symbols.get("water_loss");
    }

    public static MaterialSymbol iconWaterLux() {
        return symbols.get("water_lux");
    }

    public static MaterialSymbol iconWaterMedium() {
        return symbols.get("water_medium");
    }

    public static MaterialSymbol iconWaterOrp() {
        return symbols.get("water_orp");
    }

    public static MaterialSymbol iconWaterPh() {
        return symbols.get("water_ph");
    }

    public static MaterialSymbol iconWaterPump() {
        return symbols.get("water_pump");
    }

    public static MaterialSymbol iconWaterVoc() {
        return symbols.get("water_voc");
    }

    public static MaterialSymbol iconWaterfallChart() {
        return symbols.get("waterfall_chart");
    }

    public static MaterialSymbol iconWaves() {
        return symbols.get("waves");
    }

    public static MaterialSymbol iconWavingHand() {
        return symbols.get("waving_hand");
    }

    public static MaterialSymbol iconWbAuto() {
        return symbols.get("wb_auto");
    }

    public static MaterialSymbol iconWbCloudy() {
        return symbols.get("wb_cloudy");
    }

    public static MaterialSymbol iconWbIncandescent() {
        return symbols.get("wb_incandescent");
    }

    public static MaterialSymbol iconWbIridescent() {
        return symbols.get("wb_iridescent");
    }

    public static MaterialSymbol iconWbShade() {
        return symbols.get("wb_shade");
    }

    public static MaterialSymbol iconWbSunny() {
        return symbols.get("wb_sunny");
    }

    public static MaterialSymbol iconWbTwilight() {
        return symbols.get("wb_twilight");
    }

    public static MaterialSymbol iconWc() {
        return symbols.get("wc");
    }

    public static MaterialSymbol iconWeatherHail() {
        return symbols.get("weather_hail");
    }

    public static MaterialSymbol iconWeatherMix() {
        return symbols.get("weather_mix");
    }

    public static MaterialSymbol iconWeatherSnowy() {
        return symbols.get("weather_snowy");
    }

    public static MaterialSymbol iconWeb() {
        return symbols.get("web");
    }

    public static MaterialSymbol iconWebAsset() {
        return symbols.get("web_asset");
    }

    public static MaterialSymbol iconWebAssetOff() {
        return symbols.get("web_asset_off");
    }

    public static MaterialSymbol iconWebStories() {
        return symbols.get("web_stories");
    }

    public static MaterialSymbol iconWebTraffic() {
        return symbols.get("web_traffic");
    }

    public static MaterialSymbol iconWebhook() {
        return symbols.get("webhook");
    }

    public static MaterialSymbol iconWeekend() {
        return symbols.get("weekend");
    }

    public static MaterialSymbol iconWeight() {
        return symbols.get("weight");
    }

    public static MaterialSymbol iconWest() {
        return symbols.get("west");
    }

    public static MaterialSymbol iconWhatshot() {
        return symbols.get("whatshot");
    }

    public static MaterialSymbol iconWheelchairPickup() {
        return symbols.get("wheelchair_pickup");
    }

    public static MaterialSymbol iconWhereToVote() {
        return symbols.get("where_to_vote");
    }

    public static MaterialSymbol iconWidgetMedium() {
        return symbols.get("widget_medium");
    }

    public static MaterialSymbol iconWidgetSmall() {
        return symbols.get("widget_small");
    }

    public static MaterialSymbol iconWidgetWidth() {
        return symbols.get("widget_width");
    }

    public static MaterialSymbol iconWidgets() {
        return symbols.get("widgets");
    }

    public static MaterialSymbol iconWidth() {
        return symbols.get("width");
    }

    public static MaterialSymbol iconWidthFull() {
        return symbols.get("width_full");
    }

    public static MaterialSymbol iconWidthNormal() {
        return symbols.get("width_normal");
    }

    public static MaterialSymbol iconWidthWide() {
        return symbols.get("width_wide");
    }

    public static MaterialSymbol iconWifi() {
        return symbols.get("wifi");
    }

    public static MaterialSymbol iconWifi1Bar() {
        return symbols.get("wifi_1_bar");
    }

    public static MaterialSymbol iconWifi2Bar() {
        return symbols.get("wifi_2_bar");
    }

    public static MaterialSymbol iconWifiAdd() {
        return symbols.get("wifi_add");
    }

    public static MaterialSymbol iconWifiCalling() {
        return symbols.get("wifi_calling");
    }

    public static MaterialSymbol iconWifiCalling1() {
        return symbols.get("wifi_calling_1");
    }

    public static MaterialSymbol iconWifiCalling2() {
        return symbols.get("wifi_calling_2");
    }

    public static MaterialSymbol iconWifiCalling3() {
        return symbols.get("wifi_calling_3");
    }

    public static MaterialSymbol iconWifiCallingBar1() {
        return symbols.get("wifi_calling_bar_1");
    }

    public static MaterialSymbol iconWifiCallingBar2() {
        return symbols.get("wifi_calling_bar_2");
    }

    public static MaterialSymbol iconWifiCallingBar3() {
        return symbols.get("wifi_calling_bar_3");
    }

    public static MaterialSymbol iconWifiChannel() {
        return symbols.get("wifi_channel");
    }

    public static MaterialSymbol iconWifiFind() {
        return symbols.get("wifi_find");
    }

    public static MaterialSymbol iconWifiHome() {
        return symbols.get("wifi_home");
    }

    public static MaterialSymbol iconWifiLock() {
        return symbols.get("wifi_lock");
    }

    public static MaterialSymbol iconWifiNotification() {
        return symbols.get("wifi_notification");
    }

    public static MaterialSymbol iconWifiOff() {
        return symbols.get("wifi_off");
    }

    public static MaterialSymbol iconWifiPassword() {
        return symbols.get("wifi_password");
    }

    public static MaterialSymbol iconWifiProtectedSetup() {
        return symbols.get("wifi_protected_setup");
    }

    public static MaterialSymbol iconWifiProxy() {
        return symbols.get("wifi_proxy");
    }

    public static MaterialSymbol iconWifiTethering() {
        return symbols.get("wifi_tethering");
    }

    public static MaterialSymbol iconWifiTetheringError() {
        return symbols.get("wifi_tethering_error");
    }

    public static MaterialSymbol iconWifiTetheringOff() {
        return symbols.get("wifi_tethering_off");
    }

    public static MaterialSymbol iconWindPower() {
        return symbols.get("wind_power");
    }

    public static MaterialSymbol iconWindow() {
        return symbols.get("window");
    }

    public static MaterialSymbol iconWindowClosed() {
        return symbols.get("window_closed");
    }

    public static MaterialSymbol iconWindowOpen() {
        return symbols.get("window_open");
    }

    public static MaterialSymbol iconWindowSensor() {
        return symbols.get("window_sensor");
    }

    public static MaterialSymbol iconWindshieldDefrostAuto() {
        return symbols.get("windshield_defrost_auto");
    }

    public static MaterialSymbol iconWindshieldDefrostFront() {
        return symbols.get("windshield_defrost_front");
    }

    public static MaterialSymbol iconWindshieldDefrostRear() {
        return symbols.get("windshield_defrost_rear");
    }

    public static MaterialSymbol iconWindshieldHeatFront() {
        return symbols.get("windshield_heat_front");
    }

    public static MaterialSymbol iconWineBar() {
        return symbols.get("wine_bar");
    }

    public static MaterialSymbol iconWoman() {
        return symbols.get("woman");
    }

    public static MaterialSymbol iconWoman2() {
        return symbols.get("woman_2");
    }

    public static MaterialSymbol iconWork() {
        return symbols.get("work");
    }

    public static MaterialSymbol iconWorkAlert() {
        return symbols.get("work_alert");
    }

    public static MaterialSymbol iconWorkHistory() {
        return symbols.get("work_history");
    }

    public static MaterialSymbol iconWorkOff() {
        return symbols.get("work_off");
    }

    public static MaterialSymbol iconWorkOutline() {
        return symbols.get("work_outline");
    }

    public static MaterialSymbol iconWorkUpdate() {
        return symbols.get("work_update");
    }

    public static MaterialSymbol iconWorkflow() {
        return symbols.get("workflow");
    }

    public static MaterialSymbol iconWorkspacePremium() {
        return symbols.get("workspace_premium");
    }

    public static MaterialSymbol iconWorkspaces() {
        return symbols.get("workspaces");
    }

    public static MaterialSymbol iconWorkspacesOutline() {
        return symbols.get("workspaces_outline");
    }

    public static MaterialSymbol iconWoundsInjuries() {
        return symbols.get("wounds_injuries");
    }

    public static MaterialSymbol iconWrapText() {
        return symbols.get("wrap_text");
    }

    public static MaterialSymbol iconWrist() {
        return symbols.get("wrist");
    }

    public static MaterialSymbol iconWrongLocation() {
        return symbols.get("wrong_location");
    }

    public static MaterialSymbol iconWysiwyg() {
        return symbols.get("wysiwyg");
    }

    public static MaterialSymbol iconYakitori() {
        return symbols.get("yakitori");
    }

    public static MaterialSymbol iconYard() {
        return symbols.get("yard");
    }

    public static MaterialSymbol iconYoshoku() {
        return symbols.get("yoshoku");
    }

    public static MaterialSymbol iconYourTrips() {
        return symbols.get("your_trips");
    }

    public static MaterialSymbol iconYoutubeActivity() {
        return symbols.get("youtube_activity");
    }

    public static MaterialSymbol iconYoutubeSearchedFor() {
        return symbols.get("youtube_searched_for");
    }

    public static MaterialSymbol iconZonePersonAlert() {
        return symbols.get("zone_person_alert");
    }

    public static MaterialSymbol iconZonePersonIdle() {
        return symbols.get("zone_person_idle");
    }

    public static MaterialSymbol iconZonePersonUrgent() {
        return symbols.get("zone_person_urgent");
    }

    public static MaterialSymbol iconZoomIn() {
        return symbols.get("zoom_in");
    }

    public static MaterialSymbol iconZoomInMap() {
        return symbols.get("zoom_in_map");
    }

    public static MaterialSymbol iconZoomOut() {
        return symbols.get("zoom_out");
    }

    public static MaterialSymbol iconZoomOutMap() {
        return symbols.get("zoom_out_map");
    }
}
