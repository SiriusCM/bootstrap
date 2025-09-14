using com.sirius.bootstrap.msg;
using NativeWebSocket;
using ProtoBuf;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using UnityEngine;

// 主线程调度器（处理跨线程回调）
public class UnityMainThreadDispatcher : MonoBehaviour
{
    private static UnityMainThreadDispatcher _instance;
    private readonly Queue<Action> _executionQueue = new Queue<Action>();

    public static UnityMainThreadDispatcher Instance
    {
        get
        {
            if (_instance == null)
            {
                _instance = FindObjectOfType<UnityMainThreadDispatcher>();
                if (_instance == null)
                {
                    GameObject dispatchObj = new GameObject("UnityMainThreadDispatcher");
                    _instance = dispatchObj.AddComponent<UnityMainThreadDispatcher>();
                    DontDestroyOnLoad(dispatchObj);
                }
            }
            return _instance;
        }
    }

    private void Update()
    {
        lock (_executionQueue)
        {
            while (_executionQueue.Count > 0)
            {
                try
                {
                    _executionQueue.Dequeue().Invoke();
                }
                catch (Exception ex)
                {
                    Debug.LogError($"主线程执行错误: {ex.Message}");
                }
            }
        }
    }

    public void Enqueue(Action action)
    {
        if (action == null) return;
        lock (_executionQueue)
        {
            _executionQueue.Enqueue(action);
        }
    }
}

public class WebSocketManager : MonoBehaviour
{
    [Header("服务器配置")]
    public string ServerWsUrl = "ws://127.0.0.1:40001/game";
    public bool IsConnected { get; private set; }

    // 事件回调
    public event Action<MoveMessage> OnMoveMessageReceived;
    public event Action<bool> OnConnectionStateChanged;

    // 单例
    public static WebSocketManager Instance { get; private set; }

    private WebSocket _webSocket;
    private CancellationTokenSource _cts;
    private bool _isConnecting = false;

    private void Awake()
    {
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }
        else
        {
            Destroy(gameObject);
        }
    }

    private void Start()
    {
        StartCoroutine(ConnectCoroutine());
    }

    // 开始连接
    public void ConnectToServer()
    {
        if (IsConnected || _isConnecting) return;
        StartCoroutine(ConnectCoroutine());
    }

    private IEnumerator ConnectCoroutine()
    {
        bool needReconnect = false;
        if (IsConnected || _isConnecting) yield break;
        _isConnecting = true;

        Task connectTask = null;
        Exception connectException = null;

        // 初始化和连接逻辑（不含yield）
        try
        {
            _webSocket = new WebSocket(ServerWsUrl);

            // 注册事件回调
            _webSocket.OnOpen += OnWebSocketOpen;
            _webSocket.OnMessage += OnWebSocketMessage;
            _webSocket.OnError += OnWebSocketError;
            _webSocket.OnClose += OnWebSocketClose;

            _cts = new CancellationTokenSource();
            connectTask = _webSocket.Connect(); // 使用正确的Connect()方法（无Async后缀）
        }
        catch (Exception ex)
        {
            connectException = ex;
        }

        // 等待连接完成（yield在try外）
        if (connectTask != null)
        {
            while (!connectTask.IsCompleted)
            {
                // 检查取消信号
                if (_cts?.Token.IsCancellationRequested ?? false)
                {
                    connectException = new OperationCanceledException("连接被取消");
                    break;
                }
                yield return null; // yield不在try块内
            }

            // 处理连接结果
            if (connectTask.IsFaulted)
            {
                connectException = connectTask.Exception ?? new Exception("连接失败：未知错误");
            }
        }

        // 处理异常
        if (connectException != null)
        {
            Debug.LogError($"连接异常: {connectException.Message}");
            OnConnectionStateChanged?.Invoke(false);
            needReconnect = true;
            CleanupWebSocket();
        }

        _isConnecting = false;

        if (needReconnect)
        {
            yield return new WaitForSeconds(3f); // yield在try外
            StartCoroutine(ConnectCoroutine());
        }
    }

    // 连接成功回调
    private void OnWebSocketOpen()
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            IsConnected = true;
            Debug.Log("WebSocket 连接成功");
            OnConnectionStateChanged?.Invoke(true);
        });
    }

    // 接收消息回调
    private void OnWebSocketMessage(byte[] data)
    {
        if (data == null || data.Length == 0) return;
        HandleBinaryMessage(data);
    }

    // 错误回调
    private void OnWebSocketError(string errorMsg)
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            Debug.LogError($"WebSocket 错误: {errorMsg}");
            OnConnectionStateChanged?.Invoke(false);
            Reconnect();
        });
    }

    // 连接关闭回调
    private void OnWebSocketClose(WebSocketCloseCode code)
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            IsConnected = false;
            Debug.Log($"WebSocket 关闭，代码: {code}");
            OnConnectionStateChanged?.Invoke(false);
            Reconnect();
        });
    }

    // 发送移动消息
    public async void SendMoveMessage(MoveMessage moveMessage)
    {
        if (!IsConnected || _webSocket == null || _webSocket.State != WebSocketState.Open)
        {
            Debug.LogWarning("WebSocket 未连接或未打开，无法发送消息");
            return;
        }

        try
        {
            // 序列化消息
            Message message = new Message { moveMessage = moveMessage };
            byte[] data;
            using (MemoryStream stream = new MemoryStream())
            {
                Serializer.Serialize(stream, message);
                data = stream.ToArray();
            }

            // 异步发送消息
            await _webSocket.Send(data);
        }
        catch (Exception ex)
        {
            Debug.LogError($"发送消息异常: {ex.Message}");
            Reconnect();
        }
    }

    // 重连逻辑
    private void Reconnect()
    {
        if (!IsConnected && !_isConnecting)
        {
            Invoke(nameof(ConnectToServer), 3f);
        }
    }

    // 处理二进制消息
    private void HandleBinaryMessage(byte[] data)
    {
        try
        {
            using (MemoryStream stream = new MemoryStream(data))
            {
                Message message = Serializer.Deserialize<Message>(stream);
                if (message.moveMessage != null)
                {
                    UnityMainThreadDispatcher.Instance.Enqueue(() =>
                    {
                        OnMoveMessageReceived?.Invoke(message.moveMessage);
                    });
                }
            }
        }
        catch (Exception ex)
        {
            Debug.LogError($"消息解析失败: {ex.Message}");
        }
    }

    // 关闭连接
    public void CloseConnection()
    {
        StartCoroutine(DisconnectCoroutine());
    }

    private IEnumerator DisconnectCoroutine()
    {
        if (_webSocket == null) yield break;

        Task closeTask = null;
        Exception closeException = null;

        // 关闭逻辑（不含yield）
        try
        {
            if (_webSocket.State == WebSocketState.Open)
            {
                closeTask = _webSocket.Close();
            }
        }
        catch (Exception ex)
        {
            closeException = ex;
        }

        // 等待关闭完成（yield在try外）
        if (closeTask != null)
        {
            while (!closeTask.IsCompleted)
            {
                yield return null; // yield不在try块内
            }

            if (closeTask.IsFaulted)
            {
                closeException = closeTask.Exception ?? new Exception("关闭连接失败");
            }
        }

        // 处理关闭异常
        if (closeException != null)
        {
            Debug.LogError($"关闭连接异常: {closeException.Message}");
        }

        CleanupWebSocket();
    }

    private void CleanupWebSocket()
    {
        if (_cts != null)
        {
            _cts.Cancel();
            _cts.Dispose();
            _cts = null;
        }

        if (_webSocket != null)
        {
            // 移除事件监听
            _webSocket.OnOpen -= OnWebSocketOpen;
            _webSocket.OnMessage -= OnWebSocketMessage;
            _webSocket.OnError -= OnWebSocketError;
            _webSocket.OnClose -= OnWebSocketClose;

            _webSocket = null; // WebSocket类未实现Dispose，直接置空
        }

        IsConnected = false;
        _isConnecting = false;
        OnConnectionStateChanged?.Invoke(false);
    }

    private void OnDestroy()
    {
        if (Instance == this)
        {
            CloseConnection();
            Instance = null;
        }
    }

    // NativeWebSocket需要在每帧调用DispatchMessageQueue（非WebGL平台）
    private void Update()
    {
#if !UNITY_WEBGL || UNITY_EDITOR
        _webSocket?.DispatchMessageQueue();
#endif
    }
}