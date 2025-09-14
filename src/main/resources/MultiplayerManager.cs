using com.sirius.bootstrap.msg;
using System;
using System.Collections.Generic;
using Unity.FPS.Gameplay;
using UnityEngine;

/// <summary>
/// ����ҹ�������������Զ����ң�������Ҵӳ����л�ȡ��
/// </summary>
public class MultiplayerManager : MonoBehaviour
{
    [Header("����Һ�������")]
    public string LocalPlayerId = "player_001";  // �������ΨһID������ThirdPersonControllerһ�£�
    public GameObject RemotePlayerPrefab;        // Զ�����Ԥ���壨������/�����������
    public Transform RemotePlayerSpawnPoint;     // Զ�����Ĭ�ϳ����㣨��ѡ��
    public float RemoteSmoothTime = 0.1f;        // Զ�����λ��/��תƽ������ʱ��

    [Header("��������")]
    public bool ShowDebugLog = true;             // �Ƿ���ʾ������־

    // �洢����������ݣ�Key: PlayerId��Value: �����Ϣ��
    private Dictionary<string, PlayerData> _allPlayers = new Dictionary<string, PlayerData>();
    // ����������ã��ӳ����л�ȡ������̬���ɣ�
    private PlayerCharacterController _localPlayerController;
    private GameObject _localPlayerObj;

    // ����ģʽ��ȷ��ȫ��Ψһʵ���������ظ�����
    public static MultiplayerManager Instance { get; private set; }

    private void Awake()
    {
        // ������ʼ���������ظ�����
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject); // �л����������٣����ֶ����״̬
        }
        else
        {
            Destroy(gameObject);
            if (ShowDebugLog) Debug.LogWarning($"�Ѵ���MultiplayerManagerʵ���������ظ�����{gameObject.name}");
            return;
        }

        // ��ʼ������ֵ䣨��������ã�
        _allPlayers = new Dictionary<string, PlayerData>();

        // 1. ��ȡ��ǰʱ��������뼶��ȷ����ͬʱ�����ɵ�ID��ͬ��
        long timestamp = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

        // 2. ����4λ���������һ������ͬһ�������ظ��ĸ��ʣ�
        int randomNum = UnityEngine.Random.Range(1000, 9999);

        // 3. ���ID��ʽ���׶���Ψһ��
        LocalPlayerId = $"player_{timestamp}_{randomNum}";
    }

    private void Start()
    {
        // 1. �ӳ����л�ȡ������ң����ģ������ɣ�ֻ��ȡ��
        InitLocalPlayerFromScene();

        // 2. ��ʼ��WebSocket��Ϣ����������������ҵ�λ�ø��£�
        InitWebSocketListener();

        // 3. ��ʼ��Զ������ֵ䣨��ӱ���������ݣ�����ͳһ����
        if (_localPlayerObj != null)
        {
            AddPlayerToDict(LocalPlayerId, _localPlayerObj, isLocal: true);
            if (ShowDebugLog) Debug.Log($"��������Ѽ������ID={LocalPlayerId}��������={_localPlayerObj.name}");
        }
    }

    private void Update()
    {
        // ÿ֡ƽ����������Զ����ҵ�λ�ú���ת
        UpdateAllRemotePlayersTransform();
    }

    /// <summary>
    /// �ӳ����л�ȡ������ң�����ThirdPersonController�Ķ���
    /// </summary>
    private void InitLocalPlayerFromScene()
    {
        // ���ҳ�����Ψһ��ThirdPersonController��������ҿ�������
        _localPlayerController = FindObjectOfType<PlayerCharacterController>();

        if (_localPlayerController == null)
        {
            Debug.LogError("��MultiplayerManager��������δ�ҵ�����ThirdPersonController�ı�����Ҷ���");
            Debug.LogError("��ȷ�������д���1��������ң�������ThirdPersonController�ű�");
            return;
        }

        // ��ȡ���������Ϸ����
        _localPlayerObj = _localPlayerController.gameObject;

        // ͬ���������ID��ȷ�������������һ�£�
        if (_localPlayerController.PlayerId != LocalPlayerId)
        {
            _localPlayerController.PlayerId = LocalPlayerId;
            if (ShowDebugLog) Debug.Log($"ͬ���������ID��{_localPlayerController.PlayerId} �� {LocalPlayerId}");
        }
    }

    /// <summary>
    /// ��ʼ��WebSocket��Ϣ���������շ�����ת�����������λ����Ϣ��
    /// </summary>
    private void InitWebSocketListener()
    {
        if (WebSocketManager.Instance == null)
        {
            Debug.LogError("��MultiplayerManager��������δ�ҵ�WebSocketManagerʵ����");
            Debug.LogError("���ȴ���WebSocketManager���󣬲�����WebSocketManager�ű�");
            return;
        }

        // �����ƶ���Ϣ�¼�������������ҵ�λ�ø��£�
        WebSocketManager.Instance.OnMoveMessageReceived += HandleAllPlayerMoveMsg;
        if (ShowDebugLog) Debug.Log("��MultiplayerManager���Ѷ���WebSocket�ƶ���Ϣ�¼�");
    }

    /// <summary>
    /// ����������ҵ��ƶ���Ϣ�����ֱ���/Զ�̣�
    /// </summary>
    private void HandleAllPlayerMoveMsg(MoveMessage moveMsg)
    {
        // ������Ч��Ϣ����PlayerId��
        if (string.IsNullOrEmpty(moveMsg.playerId))
        {
            if (ShowDebugLog) Debug.LogWarning("��MultiplayerManager���յ���Ч�ƶ���Ϣ��PlayerIdΪ��");
            return;
        }

        // 1. ������ҵ���Ϣ������LocalPlayerController�Լ���������ԭ���߼���
        if (moveMsg.playerId == LocalPlayerId)
        {
            return;
        }

        // 2. Զ����ҵ���Ϣ�����������״Σ������λ�ã�������
        HandleRemotePlayerMoveMsg(moveMsg);
    }

    /// <summary>
    /// ����Զ����ҵ��ƶ���Ϣ
    /// </summary>
    private void HandleRemotePlayerMoveMsg(MoveMessage moveMsg)
    {
        string remotePlayerId = moveMsg.playerId;

        // �״��յ���Զ�������Ϣ������Զ����Ҷ���
        if (!_allPlayers.ContainsKey(remotePlayerId))
        {
            SpawnRemotePlayer(remotePlayerId);
            // ������δ�ɹ������ֵ䣬ֱ�ӷ���
            if (!_allPlayers.ContainsKey(remotePlayerId))
            {
                Debug.LogError($"��MultiplayerManager��Զ�����{remotePlayerId}����ʧ�ܣ��޷�����λ��");
                return;
            }
        }

        // ���ֵ��л�ȡԶ��������ݣ�����Ŀ��λ�ú���ת
        if (_allPlayers.TryGetValue(remotePlayerId, out PlayerData remotePlayer))
        {
            // ������Ϣ�е�λ�ú���ת��Euler��תQuaternion��
            remotePlayer.TargetPosition = new Vector3(moveMsg.posX, moveMsg.posY, moveMsg.posZ);
            remotePlayer.TargetRotation = Quaternion.Euler(moveMsg.rotX, moveMsg.rotY, moveMsg.rotZ);

            if (ShowDebugLog)
            {
                Debug.Log($"��MultiplayerManager������Զ�����λ�ã�ID={remotePlayerId}��λ��={remotePlayer.TargetPosition}");
            }
        }
    }

    /// <summary>
    /// ����Զ����Ҷ���ʹ��RemotePlayerPrefab��
    /// </summary>
    private void SpawnRemotePlayer(string playerId)
    {
        // ���Զ�����Ԥ�����Ƿ�ֵ
        if (RemotePlayerPrefab == null)
        {
            Debug.LogError("��MultiplayerManager��δ����RemotePlayerPrefab��Զ�����Ԥ���壩��");
            Debug.LogError("����Inspector����У���Զ�����Ԥ������ק��RemotePlayerPrefab�ֶ�");
            return;
        }

        // ȷ��Զ����ҳ���λ�ã�����ʹ�����õĳ����㣬����Ĭ��(0,0,0)��
        Vector3 spawnPos = RemotePlayerSpawnPoint != null
            ? RemotePlayerSpawnPoint.position
            : Vector3.zero;
        Quaternion spawnRot = RemotePlayerSpawnPoint != null
            ? RemotePlayerSpawnPoint.rotation
            : Quaternion.identity;

        // ʵ����Զ����Ҷ���
        GameObject remotePlayerObj = Instantiate(
            RemotePlayerPrefab,
            spawnPos,
            spawnRot,
            transform // ��������Ϊ�����������ڳ�������
        );

        // ����Զ����Ҷ������ƣ��������֣�
        remotePlayerObj.name = $"RemotePlayer_{playerId}";

        // �Ƴ�Զ����ҵ�����Ϳ����������������ű�����ң�
        //Destroy(remotePlayerObj.GetComponent<PlayerInput>());
        Destroy(remotePlayerObj.GetComponent<PlayerCharacterController>());
        //Destroy(remotePlayerObj.GetComponent<StarterAssetsInputs>());

        // ��Զ�������ӵ������ֵ�
        AddPlayerToDict(playerId, remotePlayerObj, isLocal: false);

        if (ShowDebugLog)
        {
            Debug.Log($"��MultiplayerManager������Զ����ң�ID={playerId}��������={remotePlayerObj.name}������λ��={spawnPos}");
        }
    }

    /// <summary>
    /// ƽ����������Զ����ҵ�λ�ú���ת
    /// </summary>
    private void UpdateAllRemotePlayersTransform()
    {
        // ����������ң�ֻ����Զ�����
        foreach (var kvp in _allPlayers)
        {
            PlayerData player = kvp.Value;

            // ����������ҺͿն���
            if (player.IsLocal || player.Avatar == null)
                continue;

            // ƽ������λ�ã�ʹ��Lerpȷ�������������������䣩
            player.Avatar.transform.position = Vector3.Lerp(
                player.Avatar.transform.position,
                player.TargetPosition,
                Time.deltaTime / RemoteSmoothTime
            );

            // ƽ��������ת��ʹ��Slerpȷ����ת����Ȼ��
            player.Avatar.transform.rotation = Quaternion.Slerp(
                player.Avatar.transform.rotation,
                player.TargetRotation,
                Time.deltaTime / RemoteSmoothTime
            );
        }
    }

    /// <summary>
    /// �������ӵ������ֵ�
    /// </summary>
    private void AddPlayerToDict(string playerId, GameObject avatarObj, bool isLocal)
    {
        if (string.IsNullOrEmpty(playerId) || avatarObj == null)
        {
            Debug.LogError("��MultiplayerManager�������ҵ��ֵ�ʧ�ܣ�PlayerIdΪ�ջ�Avatar����Ϊ��");
            return;
        }

        // �����ظ����ͬһPlayerId
        if (_allPlayers.ContainsKey(playerId))
        {
            if (ShowDebugLog) Debug.LogWarning($"��MultiplayerManager�����{playerId}�Ѵ������ֵ䣬�����ظ����");
            return;
        }

        // ����������ݲ���ӵ��ֵ�
        PlayerData newPlayer = new PlayerData
        {
            PlayerId = playerId,
            Avatar = avatarObj,
            IsLocal = isLocal,
            TargetPosition = avatarObj.transform.position,
            TargetRotation = avatarObj.transform.rotation
        };

        _allPlayers.Add(playerId, newPlayer);
    }

    /// <summary>
    /// �Ƴ�Զ����ң���Ϸ��������뿪��Ϣʹ�ã�
    /// </summary>
    public void RemoveRemotePlayer(string playerId)
    {
        if (string.IsNullOrEmpty(playerId))
        {
            Debug.LogError("��MultiplayerManager���Ƴ����ʧ�ܣ�PlayerIdΪ��");
            return;
        }

        // �������Ƴ��������
        if (playerId == LocalPlayerId)
        {
            Debug.LogError("��MultiplayerManager���������Ƴ�������ң�");
            return;
        }

        // ���ֵ��л�ȡ��������Ҷ���
        if (_allPlayers.TryGetValue(playerId, out PlayerData player))
        {
            if (player.Avatar != null)
            {
                Destroy(player.Avatar);
                if (ShowDebugLog) Debug.Log($"��MultiplayerManager���Ƴ�Զ����ң�ID={playerId}������������");
            }
            _allPlayers.Remove(playerId);
        }
        else
        {
            if (ShowDebugLog) Debug.LogWarning($"��MultiplayerManager���Ƴ����ʧ�ܣ�{playerId}���������ֵ�");
        }
    }

    /// <summary>
    /// ������Դ��ȡ���¼����ģ������ڴ�й©��
    /// </summary>
    private void OnDestroy()
    {
        if (WebSocketManager.Instance != null)
        {
            WebSocketManager.Instance.OnMoveMessageReceived -= HandleAllPlayerMoveMsg;
            if (ShowDebugLog) Debug.Log("��MultiplayerManager����ȡ��WebSocket��Ϣ����");
        }

        // ��������Զ����Ҷ���
        foreach (var kvp in _allPlayers)
        {
            if (!kvp.Value.IsLocal && kvp.Value.Avatar != null)
            {
                Destroy(kvp.Value.Avatar);
            }
        }
        _allPlayers.Clear();

        // �ͷŵ�������
        if (Instance == this)
        {
            Instance = null;
        }
    }

    /// <summary>
    /// �������ģ�ͣ��洢������ҵĺ�����Ϣ��
    /// </summary>
    [System.Serializable]
    private class PlayerData
    {
        public string PlayerId;               // ���ΨһID
        public GameObject Avatar;             // ����ڳ����е���ʾ����
        public bool IsLocal;                  // �Ƿ�Ϊ�������
        public Vector3 TargetPosition;        // Ŀ��λ�ã�����ƽ���ƶ���
        public Quaternion TargetRotation;     // Ŀ����ת������ƽ���ƶ���
    }
}