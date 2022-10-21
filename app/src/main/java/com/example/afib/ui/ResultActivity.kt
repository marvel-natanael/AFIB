package com.example.afib.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.afib.databinding.ActivityResultBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.highlight.Range


class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    private lateinit var chart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chart = binding.chart1
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);
        chart.setBackgroundColor(Color.rgb(255, 255, 255));

        // no description text
        chart.description.isEnabled = false

        // enable touch gestures
        chart.setTouchEnabled(true)

        // enable scaling and dragging
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)

        // if disabled, scaling can be done on x- and y-axis separately

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)

        chart.setDrawGridBackground(false)
        chart.maxHighlightDistance = 300f

        val x = chart.xAxis
        x.isEnabled = false

        val y = chart.axisLeft
        y.setLabelCount(6, false)
        y.textColor = Color.BLUE
        y.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        y.setDrawGridLines(false)
        y.axisLineColor = Color.BLUE

        chart.axisRight.isEnabled = false

        chart.legend.isEnabled = false

        chart.animateXY(2000, 2000)

        setData(10, 10f)

        // don't forget to refresh the drawing
        chart.invalidate();
    }

    private fun setData(count: Int, range: Float) {
        val values = ArrayList<Entry>();
        for (i in 0 until count) {
            val `val` = ((Math.random() * (range + 1)).toFloat()) + 20
            values.add(Entry(i.toFloat(), `val`))
        }

        val set1: LineDataSet

        if (chart.data != null &&
            chart.data.dataSetCount > 0
        ) {
            set1 = chart.data.getDataSetByIndex(0) as LineDataSet
            set1.values = values
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            // create a dataset and give it a type
            set1 = LineDataSet(values, "DataSet 1")
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER
            set1.cubicIntensity = 0.2f
            set1.setDrawFilled(true)
            set1.setDrawCircles(false)
            set1.lineWidth = 1.8f
            set1.circleRadius = 4f
            set1.setCircleColor(Color.WHITE)
            set1.highLightColor = Color.rgb(244, 117, 117)
            set1.color = Color.BLUE
            set1.fillColor = Color.BLUE
            set1.fillAlpha = 100
            set1.setDrawHorizontalHighlightIndicator(false)
            set1.fillFormatter =
                IFillFormatter { dataSet, dataProvider -> chart.axisLeft.axisMinimum }

            // create a data object with the data sets
            val data = LineData(set1)
            data.setValueTextSize(9f)
            data.setDrawValues(false)

            // set data
            chart.data = data
        }
    }
}