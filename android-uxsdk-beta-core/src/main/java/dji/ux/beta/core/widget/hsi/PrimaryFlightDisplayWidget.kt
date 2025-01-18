package dji.ux.beta.core.widget.hsi

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import dji.ux.beta.core.R
import dji.ux.beta.core.base.DJISDKModel
import dji.ux.beta.core.base.widget.ConstraintLayoutWidget
import dji.ux.beta.core.communication.ObservableInMemoryKeyedStore
import dji.ux.beta.core.databinding.UxsdkPrimaryFlightDisplayWidgetBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers

/**
 * Class Description
 *
 * @author Hoker
 * @date 2021/11/25
 *
 * Copyright (c) 2021, DJI All Rights Reserved.
 */
open class PrimaryFlightDisplayWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayoutWidget<PrimaryFlightDisplayWidget.ModelState>(context, attrs, defStyleAttr) {
    private lateinit var binding: UxsdkPrimaryFlightDisplayWidgetBinding

    private val widgetModel by lazy {
        PrimaryFlightDisplayModel(DJISDKModel.getInstance(), ObservableInMemoryKeyedStore.getInstance())
    }

    override fun initView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val inflater = LayoutInflater.from(context)
        binding = UxsdkPrimaryFlightDisplayWidgetBinding.inflate(inflater,this)
    }

    override fun reactToModelChanges() {
        addDisposable(widgetModel.velocityXProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setSpeedX(it.toFloat())
        })
        addDisposable(widgetModel.velocityYProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setSpeedY(it.toFloat())
        })
        addDisposable(widgetModel.velocityZProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setSpeedZ(it.toFloat())
        })
        addDisposable(widgetModel.attitudePitchProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setPitch(it.toFloat())
        })
        addDisposable(widgetModel.attitudeRollProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setRoll(it.toFloat())
        })
        addDisposable(widgetModel.attitudeYawProcessor.toFlowable().observeOn(AndroidSchedulers.mainThread()).subscribe {
            binding.fpvAttitude.setYaw(it.toFloat())
        })
        setVideoViewSize(1440, 1080)
    }

    //可以通过fpvWidget获取真实的video长宽比
    fun setVideoViewSize(videoViewWidth: Int, videoViewHeight: Int) {
        binding.fpvAttitude.setVideoViewSize(videoViewWidth, videoViewHeight)
    }

    override fun getIdealDimensionRatioString(): String? {
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
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

    sealed class ModelState
}