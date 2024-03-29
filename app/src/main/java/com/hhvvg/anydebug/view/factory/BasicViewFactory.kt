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
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.children
import androidx.core.view.isVisible
import com.hhvvg.anydebug.R
import com.hhvvg.anydebug.fragment.ViewPropertiesFragment
import com.hhvvg.anydebug.utils.Logger
import com.hhvvg.anydebug.utils.ltrb
import com.hhvvg.anydebug.utils.paddingLtrb
import com.hhvvg.anydebug.utils.reverse
import com.hhvvg.anydebug.view.ActivityPreviewWindow
import com.hhvvg.anydebug.view.PreviewList
import com.hhvvg.anydebug.view.PreviewView
import com.hhvvg.anydebug.view.SettingContent
import com.hhvvg.anydebug.view.SettingsFactory
import com.hhvvg.anydebug.view.factory.command.FactoryCommand
import com.hhvvg.anydebug.view.factory.command.HeightCommand
import com.hhvvg.anydebug.view.factory.command.MarginLtrbCommand
import com.hhvvg.anydebug.view.factory.command.PaddingLtrbCommand
import com.hhvvg.anydebug.view.factory.command.VisibilityCommand
import com.hhvvg.anydebug.view.factory.command.WidthCommand
import com.hhvvg.anydebug.view.preference.InputPreferenceView
import com.hhvvg.anydebug.view.preference.OptionsPreferenceView
import com.hhvvg.anydebug.view.preference.PreferenceView
import kotlin.reflect.KClass

/**
 * Default implementation for creating settings
 */
open class BasicViewFactory(protected val window: ActivityPreviewWindow) : SettingsFactory {

    private val commandQueue = mutableMapOf<KClass<*>, FactoryCommand>()

    override fun onCreate(
        targetView: View,
        parent: ViewGroup,
        outViews: MutableList<SettingContent>
    ) {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_basic_view_settings, parent, false)
        val preview = view.findViewById<PreviewView>(R.id.preview_item)
        val clzName = view.findViewById<PreferenceView>(R.id.view_class_name)
        val contextInfo = view.findViewById<PreferenceView>(R.id.context_info)
        val previewList = view.findViewById<PreviewList>(R.id.preview_children)
        val previewContainer = view.findViewById<ViewGroup>(R.id.preview_children_container)
        val visibilityPreference =
            view.findViewById<OptionsPreferenceView>(R.id.visibility_preference)
        val widthPreference = view.findViewById<InputPreferenceView>(R.id.width_input_preference)
        val heightPreference = view.findViewById<InputPreferenceView>(R.id.height_input_preference)
        val paddingPreference =
            view.findViewById<InputPreferenceView>(R.id.padding_ltrb_input_preference)
        val marginPreference =
            view.findViewById<InputPreferenceView>(R.id.margin_ltrb_input_preference)
        val dividerMargin = view.findViewById<View>(R.id.divider_margin)
        val removeButton = view.findViewById<View>(R.id.remove_view)
        val parentButton = view.findViewById<View>(R.id.parent_view)
        val propertiesPref = view.findViewById<View>(R.id.properties_info)

        propertiesPref.setOnClickListener {
            window.fragmentManager.push(ViewPropertiesFragment(targetView))
        }
        removeButton.isVisible = targetView.parent is View
        removeButton.setOnClickListener {
            window.mainPanelFragment.removeTargetView()
        }
        parentButton.isVisible = targetView.parent is View
        parentButton.setOnClickListener {
            window.mainPanelFragment.setTargetView(targetView.parent as View)
        }
        paddingPreference.text = targetView.paddingLtrb
        val viewParams = targetView.layoutParams
        if (viewParams is MarginLayoutParams) {
            marginPreference.isVisible = true
            dividerMargin.isVisible = true
            marginPreference.text = viewParams.ltrb
        } else {
            marginPreference.isVisible = false
            dividerMargin.isVisible = false
        }
        widthPreference.text = viewParams.width.toString()
        heightPreference.text = viewParams.height.toString()
        val visibilityIndexMapper = createVisibilityMapper()
        visibilityPreference.selectedIndex = visibilityIndexMapper[targetView.visibility] ?: 0
        preview.setRenderer(targetView)
        clzName.summary = targetView.javaClass.name
        contextInfo.summary = targetView.context.javaClass.name
        if (targetView is ViewGroup) {
            previewContainer.isVisible = true
            previewList.updatePreviewItems(targetView.children.toList())
        } else {
            previewContainer.isVisible = false
        }
        previewList.setOnPreviewClickListener {
            window.mainPanelFragment.setTargetView(it)
        }

        visibilityPreference.setOnCheckChangedListener { _, id ->
            val indexMapper = visibilityIndexMapper.reverse()
            val visibility = indexMapper[id] ?: targetView.visibility
            addCommand(VisibilityCommand(targetView, visibility))
        }
        widthPreference.setOnTextChangedListener {
            addCommand(
                WidthCommand(
                    targetView,
                    it.toString().toIntOrNull() ?: targetView.layoutParams.width
                )
            )
        }
        heightPreference.setOnTextChangedListener {
            addCommand(
                HeightCommand(
                    targetView,
                    it.toString().toIntOrNull() ?: targetView.layoutParams.height
                )
            )
        }
        paddingPreference.setOnTextChangedListener {
            addCommand(PaddingLtrbCommand(targetView, it))
        }
        marginPreference.setOnTextChangedListener {
            addCommand(MarginLtrbCommand(targetView, it))
        }
        outViews.add(
            SettingContent(
                view,
                parent.context.resources.getString(R.string.title_basic_view)
            )
        )
    }

    override fun commit() {
        commandQueue.forEach { (_, u) ->
            u.onApply()
        }
        commandQueue.clear()
    }

    protected fun addCommand(command: FactoryCommand) {
        commandQueue[command::class] = command
    }

    protected fun removeCommand(clazz: KClass<*>) {
        commandQueue.remove(clazz)
    }

    private fun createVisibilityMapper(): Map<Int, Int> {
        return mapOf(
            View.VISIBLE to 0,
            View.INVISIBLE to 1,
            View.GONE to 2
        )
    }
}