package dji.ux.beta.cameracore.widget.cameracontrols.exposuresettings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.dji.frame.util.V_JsonUtil
import dji.common.camera.CameraUtils
import dji.common.camera.ExposureSettings
import dji.common.camera.SettingsDefinitions
import dji.ux.beta.cameracore.R
import dji.ux.beta.cameracore.databinding.UxsdkWidgetIsoEiSettingBinding
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.ICameraIndex
import dji.ux.beta.core.base.SchedulerProvider
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.ui.SeekBarView
import dji.ux.beta.core.util.AudioUtil
import dji.ux.beta.core.util.CameraUtil
import dji.ux.beta.core.util.RxUtil
import dji.ux.beta.core.util.SettingDefinitions
import io.reactivex.rxjava3.functions.Action

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/2
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class ISOAndEISettingWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<ISOAndEISettingWidget.ModelState>(context, attrs, defStyleAttr),
    View.OnClickListener, SeekBarView.OnSeekBarChangeListener, ICameraIndex {

    private val LOCKED_ISO_VALUE = "500"

    private var isISOAutoSelected = false
    private var isISOAutoSupported = false
    private var isISOSeekBarEnabled = false
    private val isISOLocked = false
    private var isSeekBarTracking = false

    private var uiCameraISO = 0
    private var eiValue = 0
    private var eiRecommendedValue = 0

    //去掉auto
    private var uiIsoValueArray: Array<SettingsDefinitions.ISO?> = arrayOf()
    private var eiValueArray: IntArray = intArrayOf()
//    uxsdk_widget_iso_ei_setting
    private lateinit var binding: UxsdkWidgetIsoEiSettingBinding

    private val widgetModel by lazy {
        ISOAndEISettingModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
//        View.inflate(context, R.layout.uxsdk_widget_iso_ei_setting, this)
        val inflater = LayoutInflater.from(context)
        binding = UxsdkWidgetIsoEiSettingBinding.inflate(inflater,this)
    }

    override fun reactToModelChanges() {

        // ISO part
        addReaction(widgetModel.exposureModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            onExposureModeUpdated(it)
            updateISOEnableStatus()
        })
        addReaction(widgetModel.ISOProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            onISOUpdated(it)
        })
        addReaction(widgetModel.ISORangeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateISORangeValue(it)
            updateISOEnableStatus()
            updateISORangeUI()
        })
        addReaction(widgetModel.exposureSettingsProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            val exposureParameters = it as ExposureSettings
            uiCameraISO = exposureParameters.iso
            updateISORangeUI()
        })

        // EI part
        addReaction(widgetModel.eiValueProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })
        addReaction(widgetModel.eiValueRangeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })
        addReaction(widgetModel.eiRecommendedValueProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {

        })

        // mode
        addReaction(widgetModel.exposureSensitivityModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
        addReaction(widgetModel.cameraModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
        addReaction(widgetModel.flatCameraModeProcessor.toFlowable().observeOn(SchedulerProvider.ui()).subscribe {
            updateWidgetUI()
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // Initialize ISO array
        val res = context.resources
        val valueArray = res.getIntArray(R.array.uxsdk_iso_values)
        uiIsoValueArray = arrayOfNulls(valueArray.size)

        if (!isInEditMode) {
            for (i in valueArray.indices) {
                uiIsoValueArray[i] = SettingsDefinitions.ISO.find(valueArray[i])
            }
            updateISORangeValue(uiIsoValueArray)
        }

        // ISO seekBar
        isISOSeekBarEnabled = false
        binding.seekbarIso.progress = 0
        binding.seekbarIso.enable(false)
        binding.seekbarIso.addOnSeekBarChangeListener(this)
        binding.seekbarIso.isBaselineVisibility = false
        binding.seekbarIso.setMinValueVisibility(true)
        binding.seekbarIso.setMaxValueVisibility(true)
        binding.seekbarIso.setMinusVisibility(false)
        binding.seekbarIso.setPlusVisibility(false)
        binding.buttonIsoAuto.setOnClickListener(this)

        // EI seekBar
        binding.seekbarEi.addOnSeekBarChangeListener(this)
        binding.seekbarEi.visibility = GONE
        binding.seekbarEi.setMinValueVisibility(true)
        binding.seekbarEi.setMaxValueVisibility(true)
        binding.seekbarEi.setMinusVisibility(false)
        binding.seekbarEi.setPlusVisibility(false)

        if (!isInEditMode) {
            widgetModel.setup()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isInEditMode) {
            widgetModel.cleanup()
        }
        super.onDetachedFromWindow()
    }

    override fun getCameraIndex() = widgetModel.getCameraIndex()

    override fun getLensType() = widgetModel.getLensType()

    override fun updateCameraSource(cameraIndex: SettingDefinitions.CameraIndex, lensType: SettingsDefinitions.LensType) = widgetModel.updateCameraSource(cameraIndex, lensType)

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onProgressChanged(view: SeekBarView, progress: Int, isFromUI: Boolean) {
        if (view == binding.seekbarIso) {
            if (isISOLocked) {
                binding.seekbarIso.text = LOCKED_ISO_VALUE
            } else {
                if (uiIsoValueArray.isNotEmpty()) {
                    uiCameraISO = CameraUtils.convertISOToInt(uiIsoValueArray[progress])
                    binding.seekbarIso.text = uiCameraISO.toString()
                }
            }
        } else {
            if (progress < eiValueArray.size) {
                binding.seekbarEi.text = eiValueArray[progress].toString()
            }
        }
    }

    override fun onStartTrackingTouch(view: SeekBarView, progress: Int) {
        isSeekBarTracking = true
    }

    override fun onStopTrackingTouch(view: SeekBarView, progress: Int) {
        isSeekBarTracking = false
        AudioUtil.playSoundInBackground(context, R.raw.uxsdk_camera_ev_center)
        if (view == binding.seekbarIso) {
            if (uiIsoValueArray.isNotEmpty()) {
                val newISO = uiIsoValueArray[progress]
                newISO?.let {
                    updateISOToCamera(it)
                }
            }
        } else {
            if (progress < eiValueArray.size) {
                updateEIToCamera(eiValueArray[progress])
            }
        }
    }

    override fun onPlusClicked(view: SeekBarView) {

    }

    override fun onMinusClicked(view: SeekBarView) {

    }

    override fun onClick(v: View?) {
        if (v == binding.buttonIsoAuto) {
            isISOAutoSelected = !isISOAutoSelected
            setAutoISO(isISOAutoSelected)
        }
    }

    private fun updateWidgetUI() {
        if (widgetModel.isRecordVideoEIMode()) {
            binding.textviewIsoTitle.setText(R.string.uxsdk_camera_ei)
            binding.seekbarIsoLayout.visibility = GONE
            binding.seekbarEi.visibility = VISIBLE
        } else {
            binding.textviewIsoTitle.setText(R.string.uxsdk_camera_exposure_iso_title)
            binding.seekbarIsoLayout.visibility = VISIBLE
            binding.seekbarEi.visibility = GONE
        }
    }

    private fun onISOUpdated(iso: SettingsDefinitions.ISO) {
        if (iso == SettingsDefinitions.ISO.FIXED) {
            updateISOLocked()
        }
    }

    private fun onExposureModeUpdated(exposureMode: SettingsDefinitions.ExposureMode) {
        if (!CameraUtil.isAutoISOSupportedByProduct()) {
            if (exposureMode != SettingsDefinitions.ExposureMode.MANUAL) {
                isISOAutoSelected = true
                setAutoISO(isISOAutoSelected)
            } else {
                isISOAutoSelected = false
            }
        }
    }

    private fun updateISORangeValue(array: Array<SettingsDefinitions.ISO?>) {
        isISOAutoSupported = checkAutoISO(array)
        val newISOValues: Array<SettingsDefinitions.ISO?> = if (isISOAutoSupported) {
            arrayOfNulls(array.size - 1)
        } else {
            arrayOfNulls(array.size)
        }

        // remove the auto value
        var i = 0
        var j = 0
        while (i < array.size) {
            if (array[i] != SettingsDefinitions.ISO.AUTO) {
                newISOValues[j] = array[i]
                j++
            }
            i++
        }
        uiIsoValueArray = newISOValues
    }

    private fun updateISORangeUI() {
        // Workaround where ISO range updates to single value in AUTO mode
        if (uiIsoValueArray.isNotEmpty()) {
            val minCameraISO = CameraUtils.convertISOToInt(uiIsoValueArray[0])
            binding.seekbarIso.setMinValueText(minCameraISO.toString())
            val maxCameraISO = CameraUtils.convertISOToInt(uiIsoValueArray[uiIsoValueArray.size - 1])
            binding.seekbarIso.setMaxValueText(maxCameraISO.toString())
            binding.seekbarIso.max = uiIsoValueArray.size - 1
            isISOSeekBarEnabled = true
            updateISOValue(uiIsoValueArray, uiCameraISO)
            // Auto button has relationship with ISO range, so need update this button here.
            updateAutoISOButton()
        } else {
            isISOSeekBarEnabled = false
        }
    }

    private fun updateISOEnableStatus() {
        binding.seekbarIso.enable(!isISOAutoSelected && isISOSeekBarEnabled)
    }

    private fun checkAutoISO(array: Array<SettingsDefinitions.ISO?>): Boolean {
        for (iso in array) {
            if (iso == SettingsDefinitions.ISO.AUTO) {
                return true
            }
        }
        return false
    }

    private fun updateISOValue(array: Array<SettingsDefinitions.ISO?>, value: Int) {
        val progress: Int = getISOIndex(array, value)
        binding.seekbarIso.progress = progress
    }

    private fun updateAutoISOButton() {
        if (isISOAutoSupported && isISOSeekBarEnabled && !widgetModel.isRecordVideoEIMode() && CameraUtil.isAutoISOSupportedByProduct()) {
            binding.buttonIsoAuto.visibility = VISIBLE
        } else {
            binding.buttonIsoAuto.visibility = GONE
        }
    }

    private fun getISOIndex(array: Array<SettingsDefinitions.ISO?>, isoValue: Int): Int {
        var index = -1
        val iso = CameraUtils.convertIntToISO(isoValue)
        for (i in array.indices) {
            if (iso == array[i]) {
                index = i
                break
            }
        }
        return index
    }

    private fun updateEIRangeUI(array: IntArray) {
        // Workaround where ISO range updates to single value in AUTO mode
        if (array.isNotEmpty()) {
            binding.seekbarEi.max = array.size - 1
            binding.seekbarEi.setMinValueText(array[0].toString())
            binding.seekbarEi.setMaxValueText(array[array.size - 1].toString())
            updateEIValue(array, eiValue)
            updateEIBaseline(array, eiRecommendedValue)
        }
    }

    private fun updateEIValue(array: IntArray, eiValue: Int) {
        binding.seekbarEi.progress = getEIIndex(array, eiValue)
    }

    private fun updateEIBaseline(array: IntArray, eiRecommendedValue: Int) {
        val progress: Int = getEIIndex(array, eiRecommendedValue)
        if (progress >= 0) {
            binding.seekbarEi.baselineProgress = progress
            binding.seekbarEi.isBaselineVisibility = true
        } else {
            binding.seekbarEi.isBaselineVisibility = false
        }
    }

    private fun getEIIndex(array: IntArray, eiValue: Int): Int {
        var index = -1
        for (i in array.indices) {
            if (array[i] == eiValue) {
                index = i
                break
            }
        }
        return index
    }

    private fun setAutoISO(isAuto: Boolean) {
        var newISO: SettingsDefinitions.ISO? = null
        if (isAuto) {
            newISO = SettingsDefinitions.ISO.AUTO
        } else {
            if (binding.seekbarIso.progress < uiIsoValueArray.size) {
                newISO = uiIsoValueArray[binding.seekbarIso.progress]
            }
        }
        newISO?.let {
            updateISOToCamera(it)
        }
    }

    private fun updateISOToCamera(iso: SettingsDefinitions.ISO) {
        addDisposable(
            widgetModel.setISO(iso).observeOn(SchedulerProvider.ui()).subscribe(Action { }, RxUtil.errorConsumer({
                binding.seekbarIso.restorePreviousProgress()
            }, this.toString(), "updateISOToCamera: "))
        )
    }

    private fun updateEIToCamera(ei: Int) {
        addDisposable(
            widgetModel.setEI(ei).observeOn(SchedulerProvider.ui()).subscribe(Action { }, RxUtil.errorConsumer({
                binding.seekbarIso.restorePreviousProgress()
            }, this.toString(), "updateEIToCamera: "))
        )
    }

    // By referring to DJIGo4 in both iOS and Android version
    // Showing the ISO_FIXED  as locked value 500
    private fun updateISOLocked() {
        binding.buttonIsoAuto.visibility = GONE
        binding.seekbarIso.enable(false)
        binding.seekbarIso.progress = binding.seekbarIso.max / 2 - 1
    }

    sealed class ModelState
}