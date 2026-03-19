package io.netbird.client.ui.egress;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.netbird.client.R;
import io.netbird.client.databinding.FragmentEgressBinding;

public class EgressFragment extends Fragment {

    private FragmentEgressBinding binding;
    private EgressViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEgressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(EgressViewModel.class);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        EgressGroupAdapter adapter = new EgressGroupAdapter(group -> {
            viewModel.selectGroup(requireContext(), group);
        });

        binding.recyclerViewEgressGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewEgressGroups.setAdapter(adapter);

        binding.btnClearEgressSelection.setOnClickListener(v ->
                viewModel.clearSelection(requireContext()));

        binding.btnRetryEgress.setOnClickListener(v ->
                viewModel.loadGroups(requireContext()));

        viewModel.loadGroups(requireContext());
    }

    private void render(EgressViewModel.UiState state) {
        // Reset visibility
        binding.progressEgress.setVisibility(View.GONE);
        binding.textEgressEmpty.setVisibility(View.GONE);
        binding.textEgressError.setVisibility(View.GONE);
        binding.btnRetryEgress.setVisibility(View.GONE);
        binding.recyclerViewEgressGroups.setVisibility(View.GONE);
        binding.btnClearEgressSelection.setVisibility(View.GONE);

        switch (state.status) {
            case LOADING:
                binding.progressEgress.setVisibility(View.VISIBLE);
                break;

            case NO_AUTH:
                binding.textEgressError.setText(R.string.egress_no_auth);
                binding.textEgressError.setVisibility(View.VISIBLE);
                break;

            case ERROR:
                binding.textEgressError.setText(state.errorMessage != null
                        ? state.errorMessage : getString(R.string.egress_error));
                binding.textEgressError.setVisibility(View.VISIBLE);
                binding.btnRetryEgress.setVisibility(View.VISIBLE);
                break;

            case SUCCESS:
                List<EgressGroup> groups = state.groups;
                if (groups == null || groups.isEmpty()) {
                    binding.textEgressEmpty.setVisibility(View.VISIBLE);
                } else {
                    EgressGroupAdapter adapter = (EgressGroupAdapter)
                            binding.recyclerViewEgressGroups.getAdapter();
                    if (adapter != null) {
                        adapter.setGroups(groups, state.selectedGroupId);
                    }
                    binding.recyclerViewEgressGroups.setVisibility(View.VISIBLE);
                    if (state.selectedGroupId != null && !state.selectedGroupId.isEmpty()) {
                        binding.btnClearEgressSelection.setVisibility(View.VISIBLE);
                    }
                }
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
