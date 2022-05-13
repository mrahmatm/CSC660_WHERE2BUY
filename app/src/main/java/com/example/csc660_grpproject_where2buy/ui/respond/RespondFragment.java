package com.example.csc660_grpproject_where2buy.ui.respond;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.csc660_grpproject_where2buy.databinding.FragmentRespondBinding;

public class RespondFragment extends Fragment {

    private FragmentRespondBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RespondViewModel respondViewModel =
                new ViewModelProvider(this).get(RespondViewModel.class);

        binding = FragmentRespondBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        respondViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}