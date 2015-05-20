LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# Include res dir from chips
chips_dir := ../../../frameworks/ex/chips/res
color_picker_dir := ../../../frameworks/opt/colorpicker/res
datetimepicker_dir := ../../../frameworks/opt/datetimepicker/res
timezonepicker_dir := ../../../frameworks/opt/timezonepicker/res
res_dirs := $(chips_dir) $(color_picker_dir) $(datetimepicker_dir) $(timezonepicker_dir) res
src_dirs := src extensions_src

LOCAL_EMMA_COVERAGE_FILTER := +com.android.calendar.*

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under,$(src_dirs)) \
        /ext/src/com/mediatek/snsone/interfaces/IAccountInfo.java \
        /ext/src/com/mediatek/snsone/interfaces/IAlbumOperations.java \
        /ext/src/com/mediatek/snsone/interfaces/IContactInfo.java \
        /ext/src/com/mediatek/snsone/interfaces/IPostOperations.java

# bundled
#LOCAL_STATIC_JAVA_LIBRARIES += \
#		android-common \
#		android-common-chips \
#		calendar-common

# unbundled
# yuanding modify it for PR:669113 20140508 start
LOCAL_STATIC_JAVA_LIBRARIES := \
        android-common \
        android-common-chips \
        colorpicker \
        android-opt-datetimepicker \
        android-opt-timezonepicker \
        android-support-v4 \
        calendar-common \
        com.mediatek.calendar.ext1 \
        com.mediatek.vcalendar
# yuanding modify it for PR:669113 20140508 end
LOCAL_JAVA_LIBRARIES += mediatek-framework
#yuanding add it for PR:663743 20140430 start
LOCAL_MODULE_PATH := $(TARGET_OUT)/app/custpack
#yuanding add it for PR:663743 20140430 end
#Don't use LOCAL_SDK_VERSION.Because cann't call hide APi
#in framework when has it.
#LOCAL_SDK_VERSION := current

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_PACKAGE_NAME := Calendar

LOCAL_PROGUARD_FLAG_FILES := proguard.flags \
                             ../../../frameworks/opt/datetimepicker/proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.ex.chips
LOCAL_AAPT_FLAGS += --extra-packages com.android.colorpicker
LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker
LOCAL_AAPT_FLAGS += --extra-packages com.android.timezonepicker

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
