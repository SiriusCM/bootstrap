using com.sirius.bootstrap.msg;
using System;
using System.Collections.Generic;
using Unity.FPS.Gameplay;
using UnityEngine;

/// <summary>
/// 多玩家管理器（仅管理远程玩家，本地玩家从场景中获取）
/// </summary>
public class MultiplayerManager : MonoBehaviour
{
    [Header("多玩家核心配置")]
    public string LocalPlayerId = "player_001";  // 本地玩家唯一ID（需与ThirdPersonController一致）
    public GameObject RemotePlayerPrefab;        // 远程玩家预制体（无输入/控制器组件）
    public Transform RemotePlayerSpawnPoint;     // 远程玩家默认出生点（可选）
    public float RemoteSmoothTime = 0.1f;        // 远程玩家位置/旋转平滑过渡时间

    [Header("调试配置")]
    public bool ShowDebugLog = true;             // 是否显示调试日志

    // 存储所有玩家数据（Key: PlayerId，Value: 玩家信息）
    private Dictionary<string, PlayerData> _allPlayers = new Dictionary<string, PlayerData>();
    // 本地玩家引用（从场景中获取，不动态生成）
    private PlayerCharacterController _localPlayerController;
    private GameObject _localPlayerObj;

    // 单例模式：确保全局唯一实例，避免重复管理
    public static MultiplayerManager Instance { get; private set; }

    private void Awake()
    {
        // 单例初始化：避免重复创建
        if (Instance == null)
        {
            Instance = this;
            DontDestroyOnLoad(gameObject); // 切换场景不销毁，保持多玩家状态
        }
        else
        {
            Destroy(gameObject);
            if (ShowDebugLog) Debug.LogWarning($"已存在MultiplayerManager实例，销毁重复对象：{gameObject.name}");
            return;
        }

        // 初始化玩家字典（避免空引用）
        _allPlayers = new Dictionary<string, PlayerData>();

        // 1. 获取当前时间戳（毫秒级，确保不同时间生成的ID不同）
        long timestamp = DateTime.Now.Ticks / TimeSpan.TicksPerMillisecond;

        // 2. 生成4位随机数（进一步降低同一毫秒内重复的概率）
        int randomNum = UnityEngine.Random.Range(1000, 9999);

        // 3. 组合ID格式（易读且唯一）
        LocalPlayerId = $"player_{timestamp}_{randomNum}";
    }

    private void Start()
    {
        // 1. 从场景中获取本地玩家（核心：不生成，只获取）
        InitLocalPlayerFromScene();

        // 2. 初始化WebSocket消息监听（接收所有玩家的位置更新）
        InitWebSocketListener();

        // 3. 初始化远程玩家字典（添加本地玩家数据，便于统一管理）
        if (_localPlayerObj != null)
        {
            AddPlayerToDict(LocalPlayerId, _localPlayerObj, isLocal: true);
            if (ShowDebugLog) Debug.Log($"本地玩家已加入管理：ID={LocalPlayerId}，对象名={_localPlayerObj.name}");
        }
    }

    private void Update()
    {
        // 每帧平滑更新所有远程玩家的位置和旋转
        UpdateAllRemotePlayersTransform();
    }

    /// <summary>
    /// 从场景中获取本地玩家（挂载ThirdPersonController的对象）
    /// </summary>
    private void InitLocalPlayerFromScene()
    {
        // 查找场景中唯一的ThirdPersonController（本地玩家控制器）
        _localPlayerController = FindObjectOfType<PlayerCharacterController>();

        if (_localPlayerController == null)
        {
            Debug.LogError("【MultiplayerManager】场景中未找到挂载ThirdPersonController的本地玩家对象！");
            Debug.LogError("请确保场景中存在1个本地玩家，并挂载ThirdPersonController脚本");
            return;
        }

        // 获取本地玩家游戏对象
        _localPlayerObj = _localPlayerController.gameObject;

        // 同步本地玩家ID（确保与管理器配置一致）
        if (_localPlayerController.PlayerId != LocalPlayerId)
        {
            _localPlayerController.PlayerId = LocalPlayerId;
            if (ShowDebugLog) Debug.Log($"同步本地玩家ID：{_localPlayerController.PlayerId} → {LocalPlayerId}");
        }
    }

    /// <summary>
    /// 初始化WebSocket消息监听（接收服务器转发的所有玩家位置消息）
    /// </summary>
    private void InitWebSocketListener()
    {
        if (WebSocketManager.Instance == null)
        {
            Debug.LogError("【MultiplayerManager】场景中未找到WebSocketManager实例！");
            Debug.LogError("请先创建WebSocketManager对象，并挂载WebSocketManager脚本");
            return;
        }

        // 订阅移动消息事件（接收所有玩家的位置更新）
        WebSocketManager.Instance.OnMoveMessageReceived += HandleAllPlayerMoveMsg;
        if (ShowDebugLog) Debug.Log("【MultiplayerManager】已订阅WebSocket移动消息事件");
    }

    /// <summary>
    /// 处理所有玩家的移动消息（区分本地/远程）
    /// </summary>
    private void HandleAllPlayerMoveMsg(MoveMessage moveMsg)
    {
        // 过滤无效消息（无PlayerId）
        if (string.IsNullOrEmpty(moveMsg.playerId))
        {
            if (ShowDebugLog) Debug.LogWarning("【MultiplayerManager】收到无效移动消息：PlayerId为空");
            return;
        }

        // 1. 本地玩家的消息：交给LocalPlayerController自己处理（保持原有逻辑）
        if (moveMsg.playerId == LocalPlayerId)
        {
            return;
        }

        // 2. 远程玩家的消息：创建对象（首次）或更新位置（后续）
        HandleRemotePlayerMoveMsg(moveMsg);
    }

    /// <summary>
    /// 处理远程玩家的移动消息
    /// </summary>
    private void HandleRemotePlayerMoveMsg(MoveMessage moveMsg)
    {
        string remotePlayerId = moveMsg.playerId;

        // 首次收到该远程玩家消息：创建远程玩家对象
        if (!_allPlayers.ContainsKey(remotePlayerId))
        {
            SpawnRemotePlayer(remotePlayerId);
            // 创建后未成功加入字典，直接返回
            if (!_allPlayers.ContainsKey(remotePlayerId))
            {
                Debug.LogError($"【MultiplayerManager】远程玩家{remotePlayerId}创建失败，无法更新位置");
                return;
            }
        }

        // 从字典中获取远程玩家数据，更新目标位置和旋转
        if (_allPlayers.TryGetValue(remotePlayerId, out PlayerData remotePlayer))
        {
            // 解析消息中的位置和旋转（Euler角转Quaternion）
            remotePlayer.TargetPosition = new Vector3(moveMsg.posX, moveMsg.posY, moveMsg.posZ);
            remotePlayer.TargetRotation = Quaternion.Euler(moveMsg.rotX, moveMsg.rotY, moveMsg.rotZ);

            if (ShowDebugLog)
            {
                Debug.Log($"【MultiplayerManager】更新远程玩家位置：ID={remotePlayerId}，位置={remotePlayer.TargetPosition}");
            }
        }
    }

    /// <summary>
    /// 生成远程玩家对象（使用RemotePlayerPrefab）
    /// </summary>
    private void SpawnRemotePlayer(string playerId)
    {
        // 检查远程玩家预制体是否赋值
        if (RemotePlayerPrefab == null)
        {
            Debug.LogError("【MultiplayerManager】未设置RemotePlayerPrefab（远程玩家预制体）！");
            Debug.LogError("请在Inspector面板中，将远程玩家预制体拖拽到RemotePlayerPrefab字段");
            return;
        }

        // 确定远程玩家出生位置（优先使用配置的出生点，否则默认(0,0,0)）
        Vector3 spawnPos = RemotePlayerSpawnPoint != null
            ? RemotePlayerSpawnPoint.position
            : Vector3.zero;
        Quaternion spawnRot = RemotePlayerSpawnPoint != null
            ? RemotePlayerSpawnPoint.rotation
            : Quaternion.identity;

        // 实例化远程玩家对象
        GameObject remotePlayerObj = Instantiate(
            RemotePlayerPrefab,
            spawnPos,
            spawnRot,
            transform // 父对象设为管理器，便于场景管理
        );

        // 设置远程玩家对象名称（便于区分）
        remotePlayerObj.name = $"RemotePlayer_{playerId}";

        // 移除远程玩家的输入和控制器组件（避免干扰本地玩家）
        //Destroy(remotePlayerObj.GetComponent<PlayerInput>());
        Destroy(remotePlayerObj.GetComponent<PlayerCharacterController>());
        //Destroy(remotePlayerObj.GetComponent<StarterAssetsInputs>());

        // 将远程玩家添加到管理字典
        AddPlayerToDict(playerId, remotePlayerObj, isLocal: false);

        if (ShowDebugLog)
        {
            Debug.Log($"【MultiplayerManager】生成远程玩家：ID={playerId}，对象名={remotePlayerObj.name}，出生位置={spawnPos}");
        }
    }

    /// <summary>
    /// 平滑更新所有远程玩家的位置和旋转
    /// </summary>
    private void UpdateAllRemotePlayersTransform()
    {
        // 遍历所有玩家，只处理远程玩家
        foreach (var kvp in _allPlayers)
        {
            PlayerData player = kvp.Value;

            // 跳过本地玩家和空对象
            if (player.IsLocal || player.Avatar == null)
                continue;

            // 平滑更新位置（使用Lerp确保过渡流畅，避免跳变）
            player.Avatar.transform.position = Vector3.Lerp(
                player.Avatar.transform.position,
                player.TargetPosition,
                Time.deltaTime / RemoteSmoothTime
            );

            // 平滑更新旋转（使用Slerp确保旋转更自然）
            player.Avatar.transform.rotation = Quaternion.Slerp(
                player.Avatar.transform.rotation,
                player.TargetRotation,
                Time.deltaTime / RemoteSmoothTime
            );
        }
    }

    /// <summary>
    /// 将玩家添加到管理字典
    /// </summary>
    private void AddPlayerToDict(string playerId, GameObject avatarObj, bool isLocal)
    {
        if (string.IsNullOrEmpty(playerId) || avatarObj == null)
        {
            Debug.LogError("【MultiplayerManager】添加玩家到字典失败：PlayerId为空或Avatar对象为空");
            return;
        }

        // 避免重复添加同一PlayerId
        if (_allPlayers.ContainsKey(playerId))
        {
            if (ShowDebugLog) Debug.LogWarning($"【MultiplayerManager】玩家{playerId}已存在于字典，跳过重复添加");
            return;
        }

        // 创建玩家数据并添加到字典
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
    /// 移除远程玩家（配合服务器的离开消息使用）
    /// </summary>
    public void RemoveRemotePlayer(string playerId)
    {
        if (string.IsNullOrEmpty(playerId))
        {
            Debug.LogError("【MultiplayerManager】移除玩家失败：PlayerId为空");
            return;
        }

        // 不允许移除本地玩家
        if (playerId == LocalPlayerId)
        {
            Debug.LogError("【MultiplayerManager】不允许移除本地玩家！");
            return;
        }

        // 从字典中获取并销毁玩家对象
        if (_allPlayers.TryGetValue(playerId, out PlayerData player))
        {
            if (player.Avatar != null)
            {
                Destroy(player.Avatar);
                if (ShowDebugLog) Debug.Log($"【MultiplayerManager】移除远程玩家：ID={playerId}，对象已销毁");
            }
            _allPlayers.Remove(playerId);
        }
        else
        {
            if (ShowDebugLog) Debug.LogWarning($"【MultiplayerManager】移除玩家失败：{playerId}不存在于字典");
        }
    }

    /// <summary>
    /// 清理资源（取消事件订阅，避免内存泄漏）
    /// </summary>
    private void OnDestroy()
    {
        if (WebSocketManager.Instance != null)
        {
            WebSocketManager.Instance.OnMoveMessageReceived -= HandleAllPlayerMoveMsg;
            if (ShowDebugLog) Debug.Log("【MultiplayerManager】已取消WebSocket消息订阅");
        }

        // 销毁所有远程玩家对象
        foreach (var kvp in _allPlayers)
        {
            if (!kvp.Value.IsLocal && kvp.Value.Avatar != null)
            {
                Destroy(kvp.Value.Avatar);
            }
        }
        _allPlayers.Clear();

        // 释放单例引用
        if (Instance == this)
        {
            Instance = null;
        }
    }

    /// <summary>
    /// 玩家数据模型（存储单个玩家的核心信息）
    /// </summary>
    [System.Serializable]
    private class PlayerData
    {
        public string PlayerId;               // 玩家唯一ID
        public GameObject Avatar;             // 玩家在场景中的显示对象
        public bool IsLocal;                  // 是否为本地玩家
        public Vector3 TargetPosition;        // 目标位置（用于平滑移动）
        public Quaternion TargetRotation;     // 目标旋转（用于平滑移动）
    }
}