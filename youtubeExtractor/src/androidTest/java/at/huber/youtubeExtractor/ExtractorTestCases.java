package at.huber.youtubeExtractor;  
  
import android.os.Handler;  
import android.os.Looper;  
import android.support.test.InstrumentationRegistry;  
import android.support.test.filters.FlakyTest;  
import android.support.test.runner.AndroidJUnit4;  
import android.util.Log;  
import android.util.SparseArray;  
  
import org.junit.Test;  
import org.junit.runner.RunWith;  
  
import java.net.HttpURLConnection;  
import java.net.URL;  
import java.util.Random;  
import java.util.concurrent.CountDownLatch;  
import java.util.concurrent.TimeUnit;  
  
import static android.support.test.InstrumentationRegistry.getInstrumentation;  
import static junit.framework.TestCase.assertEquals;  
import static junit.framework.TestCase.assertNotNull;  
import static junit.framework.TestCase.assertNotSame;  
  
@RunWith(AndroidJUnit4.class)  
@FlakyTest  
public class ExtractorTestCases {  
  
    private static final String EXTRACTOR_TEST_TAG = "Extractor Test";  
    private String testUrl;  
  
    @Test  
    public void testUsualVideo() throws Throwable {  
        VideoMeta expMeta = new VideoMeta("YE7VzlLtp-4", "Big Buck Bunny", "Blender",  
                "UCSMOQeBJ2RAnuFungnQOxLg", 597, 0, false, "");  
        extractorTest("http://youtube.com/watch?v=YE7VzlLtp-4", expMeta);  
    }  
  
    @Test  
    public void testUnembeddable() throws Throwable {  
        VideoMeta expMeta = new VideoMeta("QH4VHl2uQ9o", "Match Chain Reaction Amazing Fire Art - real ghost rider", "BLACKHAND",  
                "UCl9nsRuGenStMDZfD95w85A", 331, 0, false, "");  
        extractorTest("https://www.youtube.com/watch?v=QH4VHl2uQ9o", expMeta);  
        extractorTestDashManifest("https://www.youtube.com/watch?v=QH4VHl2uQ9o");  
    }  
  
    @Test  
    public void testEncipheredVideo() throws Throwable {  
        VideoMeta expMeta = new VideoMeta("e8X3ACToii0", "Rise Against - Savior (Official Video)", "RiseAgainstVEVO",  
                "UChMKB2AHNpeuWhalpRYhUaw", 243, 0, false, "");  
        extractorTest("https://www.youtube.com/watch?v=e8X3ACToii0", expMeta);  
    }  
  
    private void extractorTestDashManifest(final String youtubeLink) throws Throwable {  
        final CountDownLatch signal = new CountDownLatch(1);  
        YouTubeExtractor.LOGGING = true;  
        YouTubeExtractor.CACHING = false;  
  
        testUrl = null;  
        new Handler(Looper.getMainLooper()).post(new Runnable() {  
            @Override  
            public void run() {  
                final YouTubeExtractor ytEx = new YouTubeExtractor(InstrumentationRegistry.getContext()) {  
                    @Override  
                    public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {  
                        assertNotNull(ytFiles);  
                        if (ytFiles != null) {  
                            int numNotDash = 0;  
                            int itag;  
                            for (int i = 0; i < ytFiles.size(); i++) {  
                                itag = ytFiles.keyAt(i);  
                                if (ytFiles.get(itag).getFormat().isDashContainer()) {  
                                    numNotDash = i;  
                                    break;  
                                }  
                            }  
                            itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size() - numNotDash) + numNotDash);  
                            testUrl = ytFiles.get(itag).getUrl();  
                            Log.d(EXTRACTOR_TEST_TAG, "Testing itag: " + itag + ", url:" + testUrl);  
                        } else {  
                            Log.e(EXTRACTOR_TEST_TAG, "YtFiles is null");  
                        }  
                        signal.countDown();  
                    }  
                };  
                ytEx.extract(youtubeLink);  
            }  
        });  
  
        signal.await(10, TimeUnit.SECONDS);  
        assertNotNull(testUrl);  
        final URL url = new URL(testUrl);  
        HttpURLConnection con = (HttpURLConnection) url.openConnection();  
        int code = con.getResponseCode();  
        con.getInputStream().close();  
        con.disconnect();  
        assertEquals(200, code);  
    }  
  
    private void extractorTest(final String youtubeLink, final VideoMeta expMeta) throws Throwable {  
        final CountDownLatch signal = new CountDownLatch(1);  
        YouTubeExtractor.LOGGING = true;  
        YouTubeExtractor.CACHING = false;  
  
        testUrl = null;  
        new Handler(Looper.getMainLooper()).post(new Runnable() {  
            @Override  
            public void run() {  
                final YouTubeExtractor ytEx = new YouTubeExtractor(getInstrumentation().getTargetContext()) {  
                    @Override  
                    public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta videoMeta) {  
                        if (videoMeta != null) {  
                            assertEquals(expMeta.getVideoId(), videoMeta.getVideoId());  
                            assertEquals(expMeta.getTitle(), videoMeta.getTitle());  
                            assertEquals(expMeta.getAuthor(), videoMeta.getAuthor());  
                            assertEquals(expMeta.getChannelId(), videoMeta.getChannelId());  
                            assertEquals(expMeta.getVideoLength(), videoMeta.getVideoLength());  
                            assertNotSame(0, videoMeta.getViewCount());  
                        } else {  
                            Log.e(EXTRACTOR_TEST_TAG, "VideoMeta is null");  
                        }  
                        assertNotNull(ytFiles);  
                        int itag = ytFiles.keyAt(new Random().nextInt(ytFiles.size()));  
                        testUrl = ytFiles.get(itag).getUrl();  
                        Log.d(EXTRACTOR_TEST_TAG, "Testing itag: " + itag + ", url:" + testUrl);  
                        signal.countDown();  
                    }  
                };  
                ytEx.extract(youtubeLink);  
            }  
        });  
  
        signal.await(10, TimeUnit.SECONDS);  
        assertNotNull(testUrl);  
        final URL url = new URL(testUrl);  
        HttpURLConnection con = (HttpURLConnection) url.openConnection();  
        int code = con.getResponseCode();  
        con.getInputStream().close();  
        con.disconnect();  
        assertEquals(200, code);  
    }  
}  
