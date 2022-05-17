package com.example.csc660_grpproject_where2buy.ui.request;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csc660_grpproject_where2buy.databinding.FragmentRequestBinding;

public class RequestFragment extends Fragment {

    private FragmentRequestBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RequestViewModel requestViewModel =
                new ViewModelProvider(this).get(RequestViewModel.class);

        binding = FragmentRequestBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView2 = binding.textView2;
        final TextView textView3 = binding.textView3;
        final EditText editText1 = binding.editTextTextPersonName;
        final ImageView imageView2 = binding.imageView2;
        final SeekBar seekBar = binding.seekBar;
        final Button button = binding.button;

//        requestViewModel.getText().observe(getViewLifecycleOwner(), textView2::setText);
//        requestViewModel.getText().observe(getViewLifecycleOwner(), textView3::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}