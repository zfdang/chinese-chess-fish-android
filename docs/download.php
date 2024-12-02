<?php
// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('memory_limit', -1);

// Function to safely output messages
function showMessage($message, $type = 'error') {
    $class = ($type == 'success') ? 'success' : 'error';
    echo "<div class='message $class'>$message</div>";
}

// Function to download and unzip a file
function downloadAndUnzip($url, $extract_path) {
    if (!filter_var($url, FILTER_VALIDATE_URL)) {
        throw new Exception('Invalid URL format: ' . htmlspecialchars($url));
    }
    
    if (empty($extract_path)) {
        throw new Exception('Extract path cannot be empty!');
    }
    
    // Create temporary file
    $temp_file = tempnam(sys_get_temp_dir(), 'zip_');
    
    try {
        $context = stream_context_create([
            'http' => [
                'timeout' => 30
            ]
        ]);
        
        $file_content = file_get_contents($url, false, $context);
        if ($file_content === false) {
            throw new Exception("Failed to download from URL: " . htmlspecialchars($url));
        }
        
        if (file_put_contents($temp_file, $file_content) === false) {
            throw new Exception("Failed to save temporary file");
        }
        
        // Create extract directory if it doesn't exist
        if (!file_exists($extract_path) && !mkdir($extract_path, 0777, true)) {
            throw new Exception("Failed to create extraction directory: " . htmlspecialchars($extract_path));
        }
        
        // Extract the zip file
        $zip = new ZipArchive();
        if ($zip->open($temp_file) !== true) {
            throw new Exception("Failed to open zip file from: " . htmlspecialchars($url));
        }
        
        if (!$zip->extractTo($extract_path)) {
            $zip->close();
            throw new Exception("Failed to extract zip contents to: " . htmlspecialchars($extract_path));
        }
        
        $zip->close();
        return true;
        
    } finally {
        // Clean up temporary file
        if (file_exists($temp_file)) {
            unlink($temp_file);
        }
    }
}
?>
<!DOCTYPE html>
<html>
<head>
    <title>Download and Unzip Files</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 800px; margin: 20px auto; padding: 20px; }
        .form-group { margin-bottom: 15px; }
        .url-group { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; border-radius: 4px; }
        input[type="text"], input[type="submit"] { padding: 8px; width: 100%; margin-top: 5px; }
        input[type="submit"] { background-color: #4CAF50; color: white; border: none; cursor: pointer; }
        input[type="submit"]:hover { background-color: #45a049; }
        .message { padding: 10px; margin: 10px 0; border-radius: 4px; }
        .success { background-color: #dff0d8; color: #3c763d; border: 1px solid #d6e9c6; }
        .error { background-color: #f2dede; color: #a94442; border: 1px solid #ebccd1; }
        h3 { margin-top: 0; color: #666; }
    </style>
</head>
<body>
    <h2>Download and Unzip Multiple Files</h2>
    
    <?php
    if ($_SERVER["REQUEST_METHOD"] == "POST") {
        $urls = array($_POST['url1'], $_POST['url2']);
        $path = "/var/www/fish/docs/apk";
        
        for ($i = 0; $i < 2; $i++) {
            if (!empty($urls[$i])) {
                try {
                    if (downloadAndUnzip($urls[$i], $path)) {
                        showMessage("File " . ($i + 1) . " successfully downloaded and extracted to: " . 
                                  htmlspecialchars($path), 'success');
                    }
                } catch (Exception $e) {
                    showMessage("Error with File " . ($i + 1) . ": " . $e->getMessage());
                }
            }
        }
    }
    ?>
    
    <form method="post" action="<?php echo htmlspecialchars($_SERVER["PHP_SELF"]); ?>">
        <div class="url-group">
            <h3>First File</h3>
            <div class="form-group">
                <label for="url1">URL to ZIP file:</label>
                <input type="text" id="url1" name="url1" 
                       value="<?php echo isset($_POST['url1']) ? htmlspecialchars($_POST['url1']) : 'https://github.com/zfdang/chinese-chess-fish-android/releases'; ?>"
                       >
            </div>            
        </div>

        <div class="url-group">
            <h3>Second File</h3>
            <div class="form-group">
                <label for="url2">URL to ZIP file:</label>
                <input type="text" id="url2" name="url2" 
                       value="<?php echo isset($_POST['url2']) ? htmlspecialchars($_POST['url2']) : 'https://github.com/zfdang/chinese-chess-fish-android/releases'; ?>"
                       >
            </div>            
        </div>
        
        <div class="form-group">
            <input type="submit" value="Download and Extract Files">
        </div>
    </form>

    <hr>
    
    <iframe src="https://fish.zfdang.com/apk/" width="100%" frameBorder="0" height="300"></iframe>
    
</body>
</html>