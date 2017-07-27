package com.sm.Zxing.Demo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.sm.Zxing.Demo.camera.CameraManager;
import com.sm.Zxing.Demo.camera.PlanarYUVLuminanceSource;
import com.sm.Zxing.Demo.decoding.CaptureActivityHandler;
import com.sm.Zxing.Demo.decoding.DecodeFormatManager;
import com.sm.Zxing.Demo.decoding.DecodeHandler;
import com.sm.Zxing.Demo.decoding.InactivityTimer;
import com.sm.Zxing.Demo.view.ViewfinderResultPointCallback;
import com.sm.Zxing.Demo.view.ViewfinderView;
import com.audace.Zxing.Demo.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CaptureActivity extends Activity implements Callback ,OnClickListener{

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private TextView txtResult;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;
	
	private Handler mHandler;
	
	private TextView mLightText;
	private ImageButton mSwitchLight;
	private ImageButton m2DCode;
	private Button mAlbum;
	private Button mSaveImage;
	private Button mCancel;
	
	private Bitmap myBitmap;//生成我的二维码
	
	private LinearLayout mShow2dCodeImage;
	private ImageView mImage2dCode;
	
	public boolean isFlashLight = false;
	
	private static final int REQUEST_CODE_OPEN_ALBUM = 0x100;
	private static final int DECODE_SUCCESS = 200;
	private static final int LIGHT_ON = 100;
	private static final int LIGHT_OFF = 99;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_zxing);
		//屏幕一直亮
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//初始化 CameraManager
		CameraManager.init(getApplication());
		
		mImage2dCode = (ImageView) findViewById(R.id.iv_zxing_my2dCode);
		mShow2dCodeImage = (LinearLayout) findViewById(R.id.ll_show_my2dCode);
		m2DCode = (ImageButton) findViewById(R.id.ib_zxing_my2dcode);
		mSaveImage = (Button) findViewById(R.id.but_zxing_myimage_save);
		mCancel = (Button) findViewById(R.id.but_zxing_myimage_cancel);
		
		mLightText = (TextView) findViewById(R.id.tv_zxing_switch_light);
		mSwitchLight = (ImageButton) findViewById(R.id.ib_zxing_switch_light);
		mAlbum = (Button) findViewById(R.id.but_album);
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		txtResult = (TextView) findViewById(R.id.txtResult);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		
		//相册按钮( 点击字体颜色改变)
		mAlbum.setOnTouchListener(new OnTouchListener() {
			
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getId() == R.id.but_album) {

					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						Log.i("TAG", "action_down....");
						mAlbum.setTextColor(Color.parseColor("#000000"));
					}
					if (event.getAction() == MotionEvent.ACTION_UP) {
						Log.i("TAG", "action_up....");
						mAlbum.setTextColor(Color.parseColor("#FFFFFF"));

						//打开系统相册，选择图片
						Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
						intent.addCategory(Intent.CATEGORY_OPENABLE);
						intent.setType("image/*");
						intent.putExtra("return-data", true);
						CaptureActivity.this.startActivityForResult(intent, REQUEST_CODE_OPEN_ALBUM);		
						
					}
					
				}
				return false;
			}
		});
		
		//底部按钮设置监听
		mSwitchLight.setOnClickListener(this);
		m2DCode.setOnClickListener(this);
		mSaveImage.setOnClickListener(this);
		mCancel.setOnClickListener(this);
		
		
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				int what = msg.what;
				if(what == DECODE_SUCCESS){
					Log.d("TAG", "decode_result---->"+msg.obj);
					//goPageURL((String) msg.obj);
					setAlbumData((String) msg.obj);
	
				}else if(what == LIGHT_ON){
					mLightText.setText("关灯");
				}else if(what == LIGHT_OFF){
					mLightText.setText("开灯");
				}
				
			}
		};
		
	}
	
	
	private void setAlbumData(String msg)
	{
		
		Intent resultIntent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("result", msg);
		resultIntent.putExtras(bundle);
		this.setResult(RESULT_OK, resultIntent);
		CaptureActivity.this.finish();
	}
	/**
	 * 获取相册选择的图片
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		Log.i("TAG", "bitmap------->"+data);
		if(requestCode == REQUEST_CODE_OPEN_ALBUM){
			if(data !=null){
				Log.i("TAG", "uri------->"+data.getData().toString());
				
//				Bitmap bitmap = (Bitmap) data.getExtras().get("data");
				
			
				//1 获取图片本地地址
				try {
					
					// 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
					ContentResolver resolver = getContentResolver();
					// 获得图片的uri（uri--->content://media/external/images/media/21275）
					Uri originalUri = data.getData(); 
					// 得到bitmap图片!!!
					final Bitmap bm = MediaStore.Images.Media.getBitmap(resolver,originalUri);
//					//得到扫描框
//					Rect frame = CameraManager.get().getFramingRect();
//					
//					//获取屏幕的宽高（display.getWidth()，display.getHeight()）
//				    WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
//				    Display display = manager.getDefaultDisplay();
//					
//					//图片缩放
//					Bitmap newbmp =  zoomBitmap(bm,frame.height(), frame.width());
//					Log.i("TAG", "newbmp:width---->"+newbmp.getWidth()+"height--->"+ newbmp.getHeight());
//					
//					//bitmap转换byte[]
//					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					newbmp.compress(Bitmap.CompressFormat.PNG,100,baos);
//					byte[] resulteData =  baos.toByteArray();
//					//解码
//					DecodeHandler.getInstance().decode(resulteData,display.getWidth(), display.getHeight(),true);
					
					new Thread(){
						@Override
						public void run() {
							Log.i("TAG", "bm------->"+bm);
							String strResult =  decodeQRImage(bm);
							if(strResult !=null){
								Message msg = new Message();
								msg.what = DECODE_SUCCESS;
								msg.obj = strResult;
								mHandler.sendMessage(msg);
							}
						};
					}.start();
					
					
					
					
					// 这里开始的第二部分，获取图片的路径：

					String[] proj = { MediaStore.Images.Media.DATA };

					// 好像是android多媒体数据库的封装接口，具体的看Android文档
					Cursor cursor = managedQuery(originalUri, proj, null, null,
							null);
					// 获得用户选择的图片的索引值
					int column_index = cursor
							.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

					// 将光标移至开头 ，这个很重要，不小心很容易引起越界
					cursor.moveToFirst();

					// 最后根据索引值获取图片路径
					String path = cursor.getString(column_index);
					Log.i("TAG", "path------->"+path);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//-----------------------------------------------------
				
				//2相册二维码图片解码
//				MultiFormatReader multiFormatReader = new MultiFormatReader();
//				Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
//				hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
//				
//			    multiFormatReader.setHints(hints);
//				 PlanarYUVLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);
//				    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
//				    try {
//				      Result rawResult = multiFormatReader.decodeWithState(bitmap);
//				    } catch (ReaderException re) {
//				      // continue
//				    } finally {
//				      multiFormatReader.reset();
//				    }
				
			}
			
		}
	
	}
	/**
	 * 缩放图片
	 * @param bitmap 原图
	 * @param width 新宽度
	 * @param height 新高度
	 * @return
	 */
	public static Bitmap zoomBitmap(Bitmap bitmap,int width,int height){
		int w  = bitmap.getWidth();
		int h = bitmap.getHeight();
		
		Matrix matrix = new Matrix();
		//计算缩放比例
		float scaleWidth = ((float) width/w);
		float scaleHeight = ((float)height/h);
		
		matrix.postScale(scaleWidth,scaleHeight);
		
		Bitmap newBitmap= Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		
		
		return newBitmap;
	}
	
	
	/**
	 * 对QRcode图片进行解码
	 * 
	 * @param bitmap
	 *            QRcode图片
	 * @return 解码后的内容
	 */
	public String decodeQRImage(Bitmap bitmap) {
		String value = null;
		Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
//		Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
//		 if (decodeFormats == null || decodeFormats.isEmpty()) {
//	    	 decodeFormats = new Vector<BarcodeFormat>();
//	    	 decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
//	    	 decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
//	    	 decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
//	    	 
//	    }	    
//	    hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
//	    if (characterSet != null) {
//	      hints.put(DecodeHintType.CHARACTER_SET, characterSet);
//	    }
//	    hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, 
//	    		new ViewfinderResultPointCallback(this.getViewfinderView()));
	  
		
		RGBLuminanceSource source = new RGBLuminanceSource(bitmap);
		BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
		QRCodeReader reader2 = new QRCodeReader();
		Result result;
		try {
			result = reader2.decode(bitmap1, hints);
			value = result.getText();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ChecksumException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return value;
	}
	
	
	/**
	 * 用字符串生成二维码
	 * @param str
	 * @author zhouzhe@lenovo-cw.com
	 * @return
	 * @throws WriterException
	 */
	public Bitmap Create2DCode(String str) throws WriterException {
		//生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
		BitMatrix matrix = new MultiFormatWriter().encode(str,BarcodeFormat.QR_CODE, 300, 300);
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		//二维矩阵转为一维像素数组,也就是一直横着排了
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if(matrix.get(x, y)){
					pixels[y * width + x] = 0xff000000;
				}else{
					pixels[y * width + x] = 0xffffffff;
				}
				
			}
		}
		
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		//通过像素数组生成bitmap,具体参考api
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	@Override
	protected void onResume() {
		super.onResume();
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			initCamera(surfaceHolder);
		} else {
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		decodeFormats = null;
		characterSet = null;

		playBeep = true;
		AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			playBeep = false;
		}
		initBeepSound();
		vibrate = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats,
					characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}
	/**
	 * 返回解码的结果
	 * @param obj 解码的内容
	 * @param barcode 扫描照片
	 */
	public void handleDecode(Result result, Bitmap barcode) {
		inactivityTimer.onActivity();
		viewfinderView.drawResultBitmap(barcode);
		 playBeepSoundAndVibrate();
		 
		 String resultString = result.getText();
			//FIXME
			if (resultString.equals("")) {
				Toast.makeText(CaptureActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
			}else {
//				System.out.println("Result:"+resultString);
				//Toast.makeText(CaptureActivity.this, resultString, Toast.LENGTH_SHORT).show();
				Intent resultIntent = new Intent();
				Bundle bundle = new Bundle();
				bundle.putString("result", resultString);
				resultIntent.putExtras(bundle);
				this.setResult(RESULT_OK, resultIntent);
				
			}
			this.finish();
		 
		 
//		txtResult.setText(obj.getBarcodeFormat().toString() + ":"
//				+ obj.getText());
		// Log.i("TAG","解码结果--->"+ obj.getBarcodeFormat().toString() + ":"+ obj.getText());
		 //goPageURL(obj.getText());
	}

	/**
	 * 跳转到网页
	 * @param result 解码结果
	 */
	private void goPageURL(String result){
		 Toast toast = Toast.makeText(this, "已扫描，正在处理...", 0);	
		 toast.setGravity(Gravity.CENTER, 0, 0);
		 toast.show();
		 //判断是否是url链接
		 if(result.contains("http://")){//是完整的url,就直接跳转
			 Uri uri = Uri.parse(result.trim());
			 Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			 startActivity(intent);
		 }else{//不是url就跳转百度搜索
			 Uri uri = Uri.parse("http://www.baidu.com/baidu?wd="+result.trim());
			 Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			 startActivity(intent);
			 
		 }
		
	}
	
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(),
						file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};



	
	public void turnBack(View v) {
		this.finish();
		
	}
	
	@Override
	public void onClick(View v) {
		if(v== mSwitchLight){//开关闪光灯
			boolean result =   CameraManager.get().onoffFlashLight();
			
			if(result){//闪光灯已经打开				
				Message msg = new Message();
				msg.what = LIGHT_ON;
				mHandler.sendMessage(msg);
			}else{
				Message msg = new Message();
				msg.what = LIGHT_OFF;
				mHandler.sendMessage(msg);
				
			}
		}else if(v == m2DCode){
			try {
				//生成我的二维码
				 myBitmap = Create2DCode("这是我的二维码！");
				 mShow2dCodeImage.setVisibility(View.VISIBLE);
				 mImage2dCode.setImageBitmap(myBitmap);
			} catch (WriterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}else if(v == mSaveImage){
			if(myBitmap != null){
				new Thread(){
					@Override
					public void run() {
						
						saveImage(myBitmap);
					}
				}.start();
			}
		}else if(v == mCancel){
			 mShow2dCodeImage.setVisibility(View.GONE);
		}
		
		
	}
	
	/**
	 * 保存图片到本地
	 * @param bitmap
	 */
	public void saveImage(Bitmap bitmap){
		//created folder
		File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+
				"/my2dcode");
		if(!folder.exists()){
			folder.mkdirs();
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(folder.getAbsolutePath()+"/mycode.jpg");
			//Bitmap.CompressFormat.PNG
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);			
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
}