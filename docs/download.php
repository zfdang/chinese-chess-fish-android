<?php
// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);

// Function to safely output messages
function showMessage($message, $type = 'error') {
    $class = ($type == 'success') ? 'success' : 'error';
    echo "<div class='message $class'>$message</div>";
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>Download and Unzip File</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 20px auto; padding: 20px; }
        .form-group { margin-bottom: 15px; }
        input[type="text"], input[type="submit"] { padding: 8px; width: 100%; margin-top: 5px; }
        input[type="submit"] { background-color: #4CAF50; color: white; border: none; cursor: pointer; }
        input[type="submit"]:hover { background-color: #45a049; }
        .message { padding: 10px; margin: 10px 0; border-radius: 4px; }
        .success { background-color: #dff0d8; color: #3c763d; border: 1px solid #d6e9c6; }
        .error { background-color: #f2dede; color: #a94442; border: 1px solid #ebccd1; }
    </style>
</head>
<body>
    <h2>Download and Unzip File</h2>
    
    <?php
    if ($_SERVER["REQUEST_METHOD"] == "POST") {
        $url = filter_input(INPUT_POST, 'url', FILTER_SANITIZE_URL);
        $extract_path = "/var/www/fish/docs/apk"
        
        // Validate inputs
        if (!filter_var($url, FILTER_VALIDATE_URL)) {
            showMessage('Invalid URL format!');
        } elseif (empty($extract_path)) {
            showMessage('Extract path cannot be empty!');
        } else {
            // Create temporary file
            $temp_file = tempnam(sys_get_temp_dir(), 'zip_');
            
            // Download the file
            try {
                $context = stream_context_create([
                    'http' => [
                        'timeout' => 30
                    ]
                ]);
                
                $file_content = file_get_contents($url, false, $context);
                if ($file_content === false) {
                    throw new Exception("Failed to download the file");
                }
                
                if (file_put_contents($temp_file, $file_content) === false) {
                    throw new Exception("Failed to save temporary file");
                }
                
                // Create extract directory if it doesn't exist
                if (!file_exists($extract_path) && !mkdir($extract_path, 0777, true)) {
                    throw new Exception("Failed to create extraction directory");
                }
                
                // Extract the zip file
                $zip = new ZipArchive();
                if ($zip->open($temp_file) !== true) {
                    throw new Exception("Failed to open zip file");
                }
                
                if (!$zip->extractTo($extract_path)) {
                    $zip->close();
                    throw new Exception("Failed to extract zip contents");
                }
                
                $zip->close();
                showMessage("File successfully downloaded and extracted to: " . htmlspecialchars($extract_path), 'success');
                
            } catch (Exception $e) {
                showMessage("Error: " . htmlspecialchars($e->getMessage()));
            } finally {
                // Clean up temporary file
                if (file_exists($temp_file)) {
                    unlink($temp_file);
                }
            }
        }
    }
    ?>
    
    <form method="post" action="<?php echo htmlspecialchars($_SERVER["PHP_SELF"]); ?>">
        <div class="form-group">
            <label for="url">URL to ZIP file:</label>
            <input type="text" id="url" name="url" required 
                   value="<?php echo isset($_POST['url']) ? htmlspecialchars($_POST['url']) : ''; ?>"
                   placeholder="https://github.com/zfdang/chinese-chess-fish-android/releases">
        </div>
                
        <div class="form-group">
            <input type="submit" value="Download and Extract">
        </div>
    </form>
</body>
</html>