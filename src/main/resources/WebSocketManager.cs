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

// ���̵߳�������������̻߳ص���
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
                    Debug.LogError($"���߳�ִ�д���: {ex.Message}");
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
    [Header("����������")]
    public string ServerWsUrl = "ws://127.0.0.1:40001/game";
    public bool IsConnected { get; private set; }

    // �¼��ص�
    public event Action<MoveMessage> OnMoveMessageReceived;
    public event Action<bool> OnConnectionStateChanged;

    // ����
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

    // ��ʼ����
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

        // ��ʼ���������߼�������yield��
        try
        {
            _webSocket = new WebSocket(ServerWsUrl);

            // ע���¼��ص�
            _webSocket.OnOpen += OnWebSocketOpen;
            _webSocket.OnMessage += OnWebSocketMessage;
            _webSocket.OnError += OnWebSocketError;
            _webSocket.OnClose += OnWebSocketClose;

            _cts = new CancellationTokenSource();
            connectTask = _webSocket.Connect(); // ʹ����ȷ��Connect()��������Async��׺��
        }
        catch (Exception ex)
        {
            connectException = ex;
        }

        // �ȴ�������ɣ�yield��try�⣩
        if (connectTask != null)
        {
            while (!connectTask.IsCompleted)
            {
                // ���ȡ���ź�
                if (_cts?.Token.IsCancellationRequested ?? false)
                {
                    connectException = new OperationCanceledException("���ӱ�ȡ��");
                    break;
                }
                yield return null; // yield����try����
            }

            // �������ӽ��
            if (connectTask.IsFaulted)
            {
                connectException = connectTask.Exception ?? new Exception("����ʧ�ܣ�δ֪����");
            }
        }

        // �����쳣
        if (connectException != null)
        {
            Debug.LogError($"�����쳣: {connectException.Message}");
            OnConnectionStateChanged?.Invoke(false);
            needReconnect = true;
            CleanupWebSocket();
        }

        _isConnecting = false;

        if (needReconnect)
        {
            yield return new WaitForSeconds(3f); // yield��try��
            StartCoroutine(ConnectCoroutine());
        }
    }

    // ���ӳɹ��ص�
    private void OnWebSocketOpen()
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            IsConnected = true;
            Debug.Log("WebSocket ���ӳɹ�");
            OnConnectionStateChanged?.Invoke(true);
        });
    }

    // ������Ϣ�ص�
    private void OnWebSocketMessage(byte[] data)
    {
        if (data == null || data.Length == 0) return;
        HandleBinaryMessage(data);
    }

    // ����ص�
    private void OnWebSocketError(string errorMsg)
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            Debug.LogError($"WebSocket ����: {errorMsg}");
            OnConnectionStateChanged?.Invoke(false);
            Reconnect();
        });
    }

    // ���ӹرջص�
    private void OnWebSocketClose(WebSocketCloseCode code)
    {
        UnityMainThreadDispatcher.Instance.Enqueue(() =>
        {
            IsConnected = false;
            Debug.Log($"WebSocket �رգ�����: {code}");
            OnConnectionStateChanged?.Invoke(false);
            Reconnect();
        });
    }

    // �����ƶ���Ϣ
    public async void SendMoveMessage(MoveMessage moveMessage)
    {
        if (!IsConnected || _webSocket == null || _webSocket.State != WebSocketState.Open)
        {
            Debug.LogWarning("WebSocket δ���ӻ�δ�򿪣��޷�������Ϣ");
            return;
        }

        try
        {
            // ���л���Ϣ
            Message message = new Message { moveMessage = moveMessage };
            byte[] data;
            using (MemoryStream stream = new MemoryStream())
            {
                Serializer.Serialize(stream, message);
                data = stream.ToArray();
            }

            // �첽������Ϣ
            await _webSocket.Send(data);
        }
        catch (Exception ex)
        {
            Debug.LogError($"������Ϣ�쳣: {ex.Message}");
            Reconnect();
        }
    }

    // �����߼�
    private void Reconnect()
    {
        if (!IsConnected && !_isConnecting)
        {
            Invoke(nameof(ConnectToServer), 3f);
        }
    }

    // �����������Ϣ
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
            Debug.LogError($"��Ϣ����ʧ��: {ex.Message}");
        }
    }

    // �ر�����
    public void CloseConnection()
    {
        StartCoroutine(DisconnectCoroutine());
    }

    private IEnumerator DisconnectCoroutine()
    {
        if (_webSocket == null) yield break;

        Task closeTask = null;
        Exception closeException = null;

        // �ر��߼�������yield��
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

        // �ȴ��ر���ɣ�yield��try�⣩
        if (closeTask != null)
        {
            while (!closeTask.IsCompleted)
            {
                yield return null; // yield����try����
            }

            if (closeTask.IsFaulted)
            {
                closeException = closeTask.Exception ?? new Exception("�ر�����ʧ��");
            }
        }

        // ����ر��쳣
        if (closeException != null)
        {
            Debug.LogError($"�ر������쳣: {closeException.Message}");
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
            // �Ƴ��¼�����
            _webSocket.OnOpen -= OnWebSocketOpen;
            _webSocket.OnMessage -= OnWebSocketMessage;
            _webSocket.OnError -= OnWebSocketError;
            _webSocket.OnClose -= OnWebSocketClose;

            _webSocket = null; // WebSocket��δʵ��Dispose��ֱ���ÿ�
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

    // NativeWebSocket��Ҫ��ÿ֡����DispatchMessageQueue����WebGLƽ̨��
    private void Update()
    {
#if !UNITY_WEBGL || UNITY_EDITOR
        _webSocket?.DispatchMessageQueue();
#endif
    }
}