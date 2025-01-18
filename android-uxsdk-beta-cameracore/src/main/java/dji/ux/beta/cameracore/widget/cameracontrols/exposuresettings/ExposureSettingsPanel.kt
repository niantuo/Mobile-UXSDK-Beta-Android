package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.databinding.UxsdkPanelExposureSettingBinding
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.util.SettingDefinitions

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/10/19
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ExposureSettingsPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ExposureSettingsPanel.ModelState>(context, attrs, defStyleAttr),
    ICameraIndex {
    //        uxsdk_panel_exposure_setting
    private lateinit var binding: UxsdkPanelExposureSettingBinding

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        View.inflate(context, R.layout.uxsdk_panel_exposure_setting, this)
        val inflater = LayoutInflater.from(context)
        binding = UxsdkPanelExposureSettingBinding.inflate(inflater,this)
    }

    override fun reactToModelChanges() {

    }

    override fun getCameraIndex() = binding.exposureSettingWidget.getCameraIndex()

    override fun getLensType() = binding.exposureSettingWidget.getLensType()

    override fun updateCameraSource(
        cameraIndex: SettingDefinitions.CameraIndex,
        lensType: SettingsDefinitions.LensType
    ) {
        binding.exposureSettingWidget.updateCameraSource(cameraIndex, lensType)
        binding.isoAndEiSettingWidget.updateCameraSource(cameraIndex, lensType)
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    sealed class ModelState
}