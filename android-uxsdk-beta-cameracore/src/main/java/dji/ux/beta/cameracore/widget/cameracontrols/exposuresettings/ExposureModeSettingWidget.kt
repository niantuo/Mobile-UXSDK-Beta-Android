package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.databinding.UxsdkWidgetExposureModeSettingBinding
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.util.RxUtil
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.functions.Action

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureModeSettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ExposureModeSettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, ICameraIndex {
    //        uxsdk_widget_exposure_mode_setting
    private lateinit var binding: UxsdkWidgetExposureModeSettingBinding

    private val widgetModel by lazy {
        ExposureModeSettingModel(
            DJISDKModel.getInstance(),
            ObservableInMemoryKeyedStore.getInstance()
        )
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        View.inflate(context, R.layout.uxsdk_widget_exposure_mode_setting, this)
        val inflater = LayoutInflater.from(context)
        binding = UxsdkWidgetExposureModeSettingBinding.inflate(inflater,this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isInEditMode) {
            widgetModel.setup()
        }
        binding.layoutCameraModeA.setOnClickListener(this)
        binding.layoutCameraModeS.setOnClickListener(this)
        binding.layoutCameraModeM.setOnClickListener(this)
        binding.layoutCameraModeP.setOnClickListener(this)
        binding.layoutCameraModeP.isSelected = true
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) = widgetModel.updateCameraSource(cameraIndex, lensType)

    override fun reactToModelChanges() {
        addReaction(
            widgetModel.exposureModeProcessor.toFlowable().observeOn(SchedulerProvider.ui())
                .subscribe {
                    updateExposureMode(it)
                })
        addReaction(
            widgetModel.exposureModeRangeProcessor.toFlowable().observeOn(SchedulerProvider.ui())
                .subscribe {
                    updateExposureModeRange(it)
                })
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onClick(v: View?) {

        val previousExposureMode: SettingsDefinitions.ExposureMode =
            widgetModel.exposureModeProcessor.value
        var exposureMode: SettingsDefinitions.ExposureMode =
            SettingsDefinitions.ExposureMode.UNKNOWN

        when (v?.id) {
            R.id.layout_camera_mode_p -> exposureMode = SettingsDefinitions.ExposureMode.PROGRAM
            R.id.layout_camera_mode_a -> exposureMode =
                SettingsDefinitions.ExposureMode.APERTURE_PRIORITY

            R.id.layout_camera_mode_s -> exposureMode =
                SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY

            R.id.layout_camera_mode_m -> exposureMode = SettingsDefinitions.ExposureMode.MANUAL
        }

        if (exposureMode == previousExposureMode) {
            return
        }

        updateExposureMode(exposureMode)

        addDisposable(
            widgetModel.setExposureMode(exposureMode)
                .observeOn(SchedulerProvider.ui())
                .subscribe(Action { }, RxUtil.errorConsumer({
                    restoreToCurrentExposureMode()
                }, this.toString(), "setExposureMode: "))
        )
    }

    private fun updateExposureModeRange(range: Array<SettingsDefinitions.ExposureMode>) {
        binding.layoutCameraModeA.isEnabled =
            rangeContains(range, SettingsDefinitions.ExposureMode.APERTURE_PRIORITY)
        binding.layoutCameraModeS.isEnabled =
            rangeContains(range, SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY)
        binding.layoutCameraModeM.isEnabled =
            rangeContains(range, SettingsDefinitions.ExposureMode.MANUAL)
        binding.layoutCameraModeP.isEnabled =
            rangeContains(range, SettingsDefinitions.ExposureMode.PROGRAM)
    }

    private fun updateExposureMode(mode: SettingsDefinitions.ExposureMode) {
        binding.layoutCameraModeA.isSelected = false
        binding.layoutCameraModeS.isSelected = false
        binding.layoutCameraModeM.isSelected = false
        binding.layoutCameraModeP.isSelected = false

        when (mode) {
            SettingsDefinitions.ExposureMode.PROGRAM -> binding.layoutCameraModeP.isSelected = true
            SettingsDefinitions.ExposureMode.SHUTTER_PRIORITY -> binding.layoutCameraModeS.isSelected =
                true

            SettingsDefinitions.ExposureMode.APERTURE_PRIORITY -> binding.layoutCameraModeA.isSelected =
                true

            SettingsDefinitions.ExposureMode.MANUAL -> binding.layoutCameraModeM.isSelected = true
            else -> {
            }
        }
    }

    private fun restoreToCurrentExposureMode() {
        updateExposureMode(widgetModel.exposureModeProcessor.value)
    }

    private fun rangeContains(
        range: Array<SettingsDefinitions.ExposureMode>?,
        value: SettingsDefinitions.ExposureMode
    ): Boolean {
        if (range == null) {
            return false
        }
        for (item in range) {
            if (item == value) {
                return true
            }
        }
        return false
    }

    sealed class ModelState
}