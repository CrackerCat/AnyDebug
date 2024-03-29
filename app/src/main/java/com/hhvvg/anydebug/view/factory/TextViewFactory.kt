/*
 *     Copyright (C) <2024>  <gitofleonardo>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.hhvvg.anydebug.view.factory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.hhvvg.anydebug.R
import com.hhvvg.anydebug.view.ActivityPreviewWindow
import com.hhvvg.anydebug.view.SettingContent
import com.hhvvg.anydebug.view.factory.command.TextCommand
import com.hhvvg.anydebug.view.factory.command.TextMaxLinesCommand
import com.hhvvg.anydebug.view.factory.command.TextSizeCommand
import com.hhvvg.anydebug.view.preference.InputPreferenceView

/**
 * Settings factory for a TextView
 */
class TextViewFactory(window: ActivityPreviewWindow) : BasicViewFactory(window) {

    override fun onCreate(
        targetView: View,
        parent: ViewGroup,
        outViews: MutableList<SettingContent>
    ) {
        super.onCreate(targetView, parent, outViews)
        if (targetView !is TextView) {
            return
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_text_view_settings, parent, false)
        val textInputPref = view.findViewById<InputPreferenceView>(R.id.text_input)
        val textSizeInputPref = view.findViewById<InputPreferenceView>(R.id.text_size_input)
        val maxLinesInputPref = view.findViewById<InputPreferenceView>(R.id.max_lines_input)

        maxLinesInputPref.text = targetView.maxLines.toString()
        maxLinesInputPref.setOnTextChangedListener {
            addCommand(TextMaxLinesCommand(targetView, it))
        }
        textInputPref.text = targetView.text
        textInputPref.setOnTextChangedListener {
            addCommand(TextCommand(targetView, it))
        }
        textSizeInputPref.text = (targetView.textSize / targetView.paint.density).toString()
        textSizeInputPref.setOnTextChangedListener {
            addCommand(TextSizeCommand(targetView, it))
        }
        outViews.add(
            SettingContent(
                view,
                parent.context.resources.getString(R.string.title_text_view)
            )
        )
    }

}