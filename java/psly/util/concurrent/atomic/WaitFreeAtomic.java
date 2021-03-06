package psly.util.concurrent.atomic;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import sun.misc.Unsafe;

public class WaitFreeAtomic {
	public final static int N = 100;
	public final static int loops = 100000;
	public static int MAX = 50;
	public final static int STEPS = 0;
	public final static int bigYields = 32;
	public final static AtomicInteger inter= new AtomicInteger();
	public final static Map<Integer, Integer> mapMaxTims = new HashMap<Integer, Integer>();
	public final static int[] ints = new int[N * loops];
//	private final static AtomicInteger inter = new AtomicInteger();
	public static void main(String[] args) throws InterruptedException {
		int errTimes = 0;
		for(int k = 0; ;) {
			ato.set(0);
			if(inter.get() != 0) {
				mapMaxTims.put(MAX, inter.get());
				MAX += 1;
				inter.set(0);
			}
			valueObj = new ValueObj(0, null);
			for(int j = 0; j < N * loops; ++j) 
				ints[j] = 0;
			final CountDownLatch latch = new CountDownLatch(1);
			Thread[] threads = new Thread[N];
			for(int i = 0; i < N; ++i) {
				threadObjs[i] = new ThreadObj(null);
				states[i] = new StateObj(STEPS);
				int threadId = i;
				(threads[i] = new Thread(){
					public void run(){
						try {
							latch.await();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						for(int j = 0; j < loops; ++j) {
							ints[getAndIncrement(threadId)] = 1;
/*							try {
								Thread.sleep(1);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}*/
						}
					}
				}).start();
			}
			long start =System.currentTimeMillis();
			latch.countDown();
			for(Thread thread: threads)
				thread.join();
			System.out.println("\n" + valueObj.value);
			for(int j = 0; j < N * loops; ++j) {
				if(ints[j] != 1) {
					System.out.println(j + " " + ints[j] + " wrong!");
					++errTimes;
				}
			}
			System.out.println("wrongTimes: " + errTimes + " MAX-TIMES: " + MAX + "-" + inter.get());
			System.out.println("times " + (++k) + " costTime: " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
			Thread.sleep(2000);
		}
	}
	final static boolean casValueObj(ValueObj cmp, ValueObj val) {
		return UNSAFE.compareAndSwapObject(valueBase, valueObjOffset, cmp, val);
	}
	
	static volatile ValueObj valueObj = new ValueObj(0, null);
	//value
	private static final Object valueBase;
	private static final long valueObjOffset;

	final static ThreadObj getThreadObj(long i) {
		return (ThreadObj) UNSAFE.getObjectVolatile(threadObjs, ((long) i << ASHIFT) + ABASE);
	}

	final static void setThreadObj(int i, ThreadObj v) {
		UNSAFE.putObjectVolatile(threadObjs, ((long) i << ASHIFT) + ABASE, v);
	}
	
	final static boolean casThreadObj(int i, ThreadObj cmp, ThreadObj finish) {
		return UNSAFE.compareAndSwapObject(threadObjs, ((long) i << ASHIFT) + ABASE, cmp, finish);
	}
	
	final static ThreadObj[]  threadObjs= new ThreadObj[N];
	final static StateObj[] states = new StateObj[N];
	private static final sun.misc.Unsafe UNSAFE;

	//thread array
	private static final int _Obase;
	private static final int _Oscale;
	private static final long ABASE;
	private static final int ASHIFT;
	
	static {
		try {
			UNSAFE = UtilUnsafe.getUnsafe();
			valueObjOffset = UNSAFE.staticFieldOffset(WaitFreeAtomic.class.getDeclaredField("valueObj"));
			valueBase = UNSAFE.staticFieldBase(WaitFreeAtomic.class.getDeclaredField("valueObj"));

			_Obase = UNSAFE.arrayBaseOffset(ThreadObj[].class);
			_Oscale = UNSAFE.arrayIndexScale(ThreadObj[].class);
			ABASE = _Obase;
			if ((_Oscale & (_Oscale - 1)) != 0)
				throw new Error("data type scale not a power of two");
			ASHIFT = 31 - Integer.numberOfLeadingZeros(_Oscale);

		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	static class ThreadObj {
		public ThreadObj(WrapperObj wrapObj) {
			super();
			this.wrapperObj = wrapObj;
		}
		
		WrapperObj wrapperObj;
		long[] longs = new long[16];
		static final class WrapperObj {
			final ValueObj value;
			final boolean isFinish;
			public WrapperObj(ValueObj value, boolean isFinish) {
				super();
				this.value = value;
				this.isFinish = isFinish;
			}
		}
		
		boolean casWrapValue(WrapperObj cmp, WrapperObj val) {
			return UNSAFE.compareAndSwapObject(this, wrapValueOffset, cmp, val);
		}
		
		void putWrapValueVolatile(WrapperObj val) {
			UNSAFE.putObjectVolatile(this, wrapValueOffset, val);
		}
		
		private static final sun.misc.Unsafe UNSAFE;
		private static final long wrapValueOffset;
		static {
			try {
				UNSAFE = UtilUnsafe.getUnsafe();
				wrapValueOffset = UNSAFE.objectFieldOffset(ThreadObj.class.getDeclaredField("wrapperObj"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
	
	private static class StateObj {
		public StateObj(int assistStep) {
			super();
			this.assistStep = assistStep;
			this.steps = 0;
			this.index = 0;
		}
		
		private final int assistStep;
		private int steps;
		private long index;
	}
	
	private static class ValueObj {
		private final int value;
		private final ThreadObj threadObj;
		public ValueObj(int value, ThreadObj threadObj) {
			super();
			this.value = value;
			this.threadObj = threadObj;
		}
		
	}
	
	public static AtomicInteger ato = new AtomicInteger();
	public static int getAndIncrementFast(int index) {
		//Thread.yield();
		return ato.getAndIncrement();
	}
	
    public static int getAndIncrement(int index) {
        //fast-path， 最多MAX次。
        int count = MAX;
        for(;;) {
            ValueObj valueObj_ = valueObj;
            if(valueObj_.threadObj == null) {
                ValueObj valueObjNext = new ValueObj(valueObj_.value + 1, null);
                if(casValueObj(valueObj_, valueObjNext)) {
                    StateObj myState = states[index];
                    //前进一步，每assistStep，尝试一个帮助。
                    if(((++myState.steps) & myState.assistStep) == 0){
                        long helpThread = myState.index;
                        help(helpThread);
                        //下一个协助的对象。
                        ++myState.index;
                    }
                    return valueObj_.value;
                }
                Thread.yield();Thread.yield();Thread.yield();Thread.yield();
            } else {
                helpTransfer(valueObj_);
            }
            
            if(--count == 0)
                break;
        }
//        System.out.println("here " + inter.incrementAndGet());
        inter.incrementAndGet();
        for(int j = 0; j < bigYields; ++j)
            Thread.yield();
        
        //slow-path，将自己列为被帮助对象。
        ThreadObj myselfObj = new ThreadObj(new ThreadObj.WrapperObj(null, false));
        setThreadObj(index, myselfObj);
        //开始帮助自己
        ValueObj result = help(index);
        setThreadObj(index, null);
        return result.value;
    }
    
    // valueObj->threadObj->wrapperObj->valueObj。
    // step 1-3，每一个步骤都不会阻塞其他步骤。
    // 严格遵守以下顺序: 
    // step 1: 通过将ValueObj指向ThreadObj:
    //         atomic: (value, null)->(value, ThreadObj)来锚定该值                      //确定该value归ThreadObj对应线程所有。
    // step 2: 通过将ThreadObj包裹的WrapperObj，
    //         atomic: 从(null, false)更新为(valueObj, true)来更新状态的同时传递value    //对应线程通过isFinish判定操作已完成。
    // step 3: 更新ValueObj，提升value，同时设置ThreadObj为null：
    //         atomic: (value, ThreadObj)->(value+1, null)完成收尾动作                 //此时value值回到了没有被线程锚定的状态，也可以看做step1之前的状态。
    private static ValueObj help(long helpIndex) {
        helpIndex = helpIndex % N;
        ThreadObj helpObj = getThreadObj(helpIndex);
        ThreadObj.WrapperObj wrapperObj;
        if(helpObj == null || helpObj.wrapperObj == null)
            return null;
        //判定句，是否该线程对应的操作未完成，(先取valueObj，再取isFinish，这很重要)。
        ValueObj valueObj_ = valueObj;
        while(!(wrapperObj = helpObj.wrapperObj).isFinish) {
            /*ValueObj valueObj_ = valueObj;*/
            if(valueObj_.threadObj == null) {
                ValueObj intermediateObj = new ValueObj(valueObj_.value, helpObj);
                //step1
                if(!casValueObj(valueObj_, intermediateObj)) {
                    valueObj_ = valueObj;
                    continue;
                }
                //step1: 锚定该ValueObj，接下来所有看到该valueObj的线程，都会一致地完成一系列操作.
                valueObj_ = intermediateObj;
            }
            //完成ValueObj、ThreadObj中的WrapperObj的状态迁移。
            helpTransfer(valueObj_);
            valueObj_ = valueObj;
        }
        valueObj_ = wrapperObj.value;
        helpValueTransfer(valueObj_);
        //返回锚定的valueObj。
        return valueObj_;
    }
    
    private static void helpTransfer(ValueObj valueObj_) {
        ThreadObj.WrapperObj wrapperObj = valueObj_.threadObj.wrapperObj;
        //step2: 先完成ThreadObj的状态迁移，WrapperObj(valueObj，true)分别表示(值，完成)，原子地将这两个值喂给threadObj。
        if(!wrapperObj.isFinish) {
            ThreadObj.WrapperObj wrapValueFiniash = new ThreadObj.WrapperObj(valueObj_, true);
             valueObj_.threadObj.casWrapValue(wrapperObj, wrapValueFiniash);
            // or
            //valueObj_.threadObj.putWrapValueVolatile(wrapValueFiniash);
        }
        //step3: 最后完成ValueObj上的状态迁移
        helpValueTransfer(valueObj_);
    }
    
    private static ValueObj helpValueTransfer(ValueObj valueObj_) {
        if(valueObj_ == valueObj) {
            ValueObj valueObjNext = new ValueObj(valueObj_.value + 1, null);
            casValueObj(valueObj_, valueObjNext);
        }
        return valueObj_;
    }
	
	private static class UtilUnsafe {
		private UtilUnsafe() {
		}

		public static Unsafe getUnsafe() {
			if (UtilUnsafe.class.getClassLoader() == null)
				return Unsafe.getUnsafe();
			try {
				final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
				fld.setAccessible(true);
				return (Unsafe) fld.get(UtilUnsafe.class);
			} catch (Exception e) {
				throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
			}
		}
	}
}
