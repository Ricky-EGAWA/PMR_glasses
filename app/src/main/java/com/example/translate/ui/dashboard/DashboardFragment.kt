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
        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    // user choose image
                    val imageUri = uri
                    Toast.makeText(requireContext(), "URI: $imageUri", Toast.LENGTH_LONG).show()

                    // show URI in TextView
                    imageUriTextView.text = "URI: \n$imageUri"

                    // show image in ImageView
                    selectedImageView.setImageURI(imageUri)


                    Glide.with(this).load(imageUri).into(selectedImageView)


                    val recognizeText = RecognizeText()
                    recognizeText.recognizeText(
                        lang = "la",
                        context = requireContext(),
                        uri = imageUri
                    ) { text ->
                        recognizeTextView.text = text.text
                        Log.i("RecognizedText", text.text)
                    }

                } else {
                    // canceled the selection
                    Toast.makeText(requireContext(), "Select canceled", Toast.LENGTH_SHORT).show()
                    imageUriTextView.text = "URI: non"
                    selectedImageView.setImageURI(null) // vide ImageView
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
            // image selector
            pickImageLauncher.launch("image/*")
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}