package io.netbird.client.ui.setup;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import io.netbird.client.PlatformUtils;
import io.netbird.client.R;
import io.netbird.client.ServiceAccessor;
import io.netbird.client.databinding.FragmentSetupBinding;
import io.netbird.client.ui.PreferenceUI;

public class SetupFragment extends Fragment {

    private FragmentSetupBinding binding;
    private SetupViewModel viewModel;
    private ServiceAccessor serviceAccessor;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null && binding != null) {
                    binding.editTextSetupKey.setText(result.getContents().trim());
                }
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchScanner();
                } else {
                    Toast.makeText(requireContext(),
                            R.string.setup_camera_denied, Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ServiceAccessor) {
            serviceAccessor = (ServiceAccessor) context;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SetupViewModel.class);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);

        // Block back navigation until the user has a valid registration
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (!PreferenceUI.isRegistered(requireContext())) {
                            // Do nothing — registration is required before proceeding
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        if (PlatformUtils.isAndroidTV(requireContext())) {
            binding.btnScanQr.setVisibility(View.GONE);
            binding.dividerOr.setVisibility(View.GONE);
        }

        binding.btnScanQr.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchScanner();
            } else {
                cameraPermLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnConnect.setOnClickListener(v -> submit());

        binding.editTextSetupKey.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
                hideKeyboard(v);
                submit();
                return true;
            }
            return false;
        });

        if (!PlatformUtils.isAndroidTV(requireContext())) {
            binding.editTextSetupKey.requestFocus();
            binding.editTextSetupKey.post(() -> {
                InputMethodManager imm = (InputMethodManager)
                        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.editTextSetupKey, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    private void submit() {
        String key = binding.editTextSetupKey.getText().toString().trim();
        if (key.isEmpty()) {
            binding.textInputSetupKey.setError(getString(R.string.setup_key_required));
            return;
        }
        binding.textInputSetupKey.setError(null);
        viewModel.loginWithSetupKey(requireContext(), key);
    }

    private void render(SetupViewModel.UiState state) {
        switch (state.status) {
            case LOADING:
                binding.progressSetup.setVisibility(View.VISIBLE);
                binding.btnConnect.setEnabled(false);
                binding.btnScanQr.setEnabled(false);
                binding.editTextSetupKey.setEnabled(false);
                break;

            case SUCCESS:
                if (serviceAccessor != null) serviceAccessor.stopEngine();
                requireActivity().getSupportFragmentManager().popBackStack();
                break;

            case ERROR:
                binding.progressSetup.setVisibility(View.GONE);
                binding.btnConnect.setEnabled(true);
                binding.btnScanQr.setEnabled(true);
                binding.editTextSetupKey.setEnabled(true);
                binding.textInputSetupKey.setError(state.errorMessage);
                break;

            case IDLE:
                binding.progressSetup.setVisibility(View.GONE);
                break;
        }
    }

    private void launchScanner() {
        scanLauncher.launch(new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(getString(R.string.setup_scan_prompt))
                .setBeepEnabled(false)
                .setOrientationLocked(false));
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
