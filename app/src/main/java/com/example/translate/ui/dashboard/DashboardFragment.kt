package com.example.translate.ui.dashboard

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.translate.RecognizeText
import com.example.translate.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    private lateinit var selectImageButton: Button
    private lateinit var selectedImageView: ImageView
    private lateinit var imageUriTextView: TextView
    private lateinit var recognizeTextView: TextView

    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                // 用户选择了图片
                val imageUri = uri
                Toast.makeText(requireContext(), "URI: $imageUri", Toast.LENGTH_LONG).show()

                // 将URI显示在TextView中
                imageUriTextView.text = "URI: \n$imageUri"

                // 将图片显示在ImageView中
                selectedImageView.setImageURI(imageUri) // 直接设置URI即可显示图片

                // 如果使用Glide/Coil等库加载图片，可以这样做：
                Glide.with(this).load(imageUri).into(selectedImageView)
                // 或者 Coil: selectedImageView.load(imageUri)

                // 此时，你已经获取到了图片的Uri (imageUri)。
                // 你可以将其传递给ViewModel，上传到服务器，或者进行其他操作。
                val recognizeText = RecognizeText()
                recognizeText.recognizeText(lang = "la", context = requireContext(), uri = imageUri) { text ->
                    recognizeTextView.text = text.text
                    Log.i("RecognizedText",text.text)
                }

            } else {
                // 用户取消了图片选择
                Toast.makeText(requireContext(), "Select canceled", Toast.LENGTH_SHORT).show()
                imageUriTextView.text = "URI: non"
                selectedImageView.setImageURI(null) // 清空ImageView
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        selectedImageView = binding.selectedImageView
        selectImageButton = binding.selectImageButton
        imageUriTextView = binding.imageUriTextView
        recognizeTextView = binding.recognizeTextView

        selectImageButton.setOnClickListener {
            // 触发图片选择器
            // "image/*" 表示选择所有类型的图片
            pickImageLauncher.launch("image/*")
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}